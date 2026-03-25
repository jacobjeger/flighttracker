const express = require('express');
const cron = require('node-cron');
const fetch = require('node-fetch');
const admin = require('firebase-admin');

const app = express();
app.use(express.json());

// Initialize Firebase Admin SDK
// In production, set GOOGLE_APPLICATION_CREDENTIALS env var or use service account
if (process.env.FIREBASE_SERVICE_ACCOUNT) {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
} else {
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
  });
}

const FLIGHTAWARE_API_KEY = process.env.FLIGHTAWARE_API_KEY || 'YOUR_API_KEY';
const FLIGHTAWARE_BASE_URL = 'https://aeroapi.flightaware.com/aeroapi';
const PORT = process.env.PORT || 3000;

// In-memory store (use Redis/DB in production)
const deviceStore = new Map(); // fcmToken -> { trackedFlights: Set<flightId> }
const flightStore = new Map(); // flightId -> { lastStatus, gate_origin, gate_destination, terminal_origin, terminal_destination, delay, baggage_claim }

// --- API Routes ---

// Register device with FCM token and tracked flights
app.post('/api/register', (req, res) => {
  const { fcmToken, trackedFlights } = req.body;
  if (!fcmToken) {
    return res.status(400).json({ success: false, message: 'fcmToken required' });
  }

  deviceStore.set(fcmToken, {
    trackedFlights: new Set(trackedFlights || []),
  });

  console.log(`Device registered: ${fcmToken.substring(0, 20)}... with ${(trackedFlights || []).length} flights`);
  res.json({ success: true, message: 'Device registered' });
});

// Add a tracked flight for a device
app.post('/api/track', (req, res) => {
  const { fcmToken, flightId } = req.body;
  if (!fcmToken || !flightId) {
    return res.status(400).json({ success: false, message: 'fcmToken and flightId required' });
  }

  let device = deviceStore.get(fcmToken);
  if (!device) {
    device = { trackedFlights: new Set() };
    deviceStore.set(fcmToken, device);
  }
  device.trackedFlights.add(flightId);

  console.log(`Flight tracked: ${flightId} for device ${fcmToken.substring(0, 20)}...`);
  res.json({ success: true, message: 'Flight tracked' });
});

// Remove a tracked flight for a device
app.post('/api/untrack', (req, res) => {
  const { fcmToken, flightId } = req.body;
  if (!fcmToken || !flightId) {
    return res.status(400).json({ success: false, message: 'fcmToken and flightId required' });
  }

  const device = deviceStore.get(fcmToken);
  if (device) {
    device.trackedFlights.delete(flightId);
  }

  console.log(`Flight untracked: ${flightId} for device ${fcmToken.substring(0, 20)}...`);
  res.json({ success: true, message: 'Flight untracked' });
});

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', trackedDevices: deviceStore.size, trackedFlights: flightStore.size });
});

// --- Flight Polling ---

async function fetchFlightData(flightId) {
  try {
    const response = await fetch(`${FLIGHTAWARE_BASE_URL}/flights/${flightId}`, {
      headers: { 'x-apikey': FLIGHTAWARE_API_KEY },
    });

    if (!response.ok) {
      console.error(`FlightAware API error for ${flightId}: ${response.status}`);
      return null;
    }

    const data = await response.json();
    return data.flights?.[0] || null;
  } catch (error) {
    console.error(`Error fetching flight ${flightId}:`, error.message);
    return null;
  }
}

function getFlightStatus(flight) {
  if (flight.cancelled) return 'Cancelled';
  const progress = flight.progress_percent || 0;
  const status = flight.status || '';

  if (status.toLowerCase().includes('cancelled')) return 'Cancelled';
  if (status.toLowerCase().includes('landed') || status.toLowerCase().includes('arrived')) return 'Landed';
  if (progress > 0 && progress < 100) return 'En Route';
  if (flight.departure_delay && flight.departure_delay > 0) return 'Delayed';
  if (status.toLowerCase().includes('delayed')) return 'Delayed';
  return 'On Time';
}

function getDelayMinutes(flight) {
  return Math.round((flight.departure_delay || flight.arrival_delay || 0) / 60);
}

async function sendNotification(fcmToken, flightId, title, body, type) {
  try {
    await admin.messaging().send({
      token: fcmToken,
      data: {
        flight_id: flightId,
        title,
        body,
        type,
      },
      notification: {
        title,
        body,
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'flight_updates',
          priority: 'max',
        },
      },
    });
    console.log(`Notification sent: ${title} to ${fcmToken.substring(0, 20)}...`);
  } catch (error) {
    console.error(`FCM send error for ${fcmToken.substring(0, 20)}...:`, error.message);
    // Remove invalid tokens
    if (error.code === 'messaging/invalid-registration-token' ||
        error.code === 'messaging/registration-token-not-registered') {
      deviceStore.delete(fcmToken);
      console.log(`Removed invalid token: ${fcmToken.substring(0, 20)}...`);
    }
  }
}

async function checkFlightUpdates() {
  // Collect all unique flight IDs being tracked
  const allFlightIds = new Set();
  for (const [, device] of deviceStore) {
    for (const flightId of device.trackedFlights) {
      allFlightIds.add(flightId);
    }
  }

  console.log(`Checking ${allFlightIds.size} tracked flights...`);

  for (const flightId of allFlightIds) {
    const flight = await fetchFlightData(flightId);
    if (!flight) continue;

    const currentStatus = getFlightStatus(flight);
    const currentDelay = getDelayMinutes(flight);
    const flightIdent = flight.ident_iata || flight.ident || flightId;

    const prev = flightStore.get(flightId) || {};
    const changes = [];

    // Check status change
    if (prev.status && prev.status !== currentStatus) {
      if (currentStatus === 'Cancelled') {
        changes.push({
          type: 'cancellation',
          title: `✈️ ${flightIdent} — Flight CANCELLED`,
          body: `${flightIdent} has been cancelled.`,
        });
      } else if (currentStatus === 'Landed') {
        const baggageText = flight.baggage_claim ? ` Baggage at Carousel ${flight.baggage_claim}.` : '';
        changes.push({
          type: 'status_change',
          title: `✈️ ${flightIdent} — Landed`,
          body: `${flightIdent} has landed.${baggageText}`,
        });
      } else {
        changes.push({
          type: 'status_change',
          title: `✈️ ${flightIdent} — ${currentStatus}`,
          body: currentStatus === 'Delayed'
            ? `${flightIdent} now delayed ${currentDelay} minutes.`
            : `${flightIdent} is now ${currentStatus}.`,
        });
      }
    }

    // Check delay increase (15+ min)
    if (prev.delay !== undefined && currentDelay - prev.delay >= 15) {
      const depTime = flight.estimated_out || flight.estimated_off || '';
      changes.push({
        type: 'delay_increase',
        title: `✈️ ${flightIdent} — Now delayed ${currentDelay} minutes`,
        body: depTime ? `New departure: ${depTime.substring(11, 16)}` : `Delay increased to ${currentDelay} minutes.`,
      });
    }

    // Check departure gate change
    if (prev.gate_origin && flight.gate_origin && prev.gate_origin !== flight.gate_origin) {
      changes.push({
        type: 'gate_change',
        title: `✈️ ${flightIdent} — Gate changed to ${flight.gate_origin}`,
        body: `Departure gate changed to ${flight.gate_origin}. Was ${prev.gate_origin}.`,
      });
    }

    // Check arrival gate change
    if (prev.gate_destination && flight.gate_destination && prev.gate_destination !== flight.gate_destination) {
      changes.push({
        type: 'gate_change',
        title: `✈️ ${flightIdent} — Arrival gate changed to ${flight.gate_destination}`,
        body: `Arrival gate changed to ${flight.gate_destination}. Was ${prev.gate_destination}.`,
      });
    }

    // Check departure terminal change
    if (prev.terminal_origin && flight.terminal_origin && prev.terminal_origin !== flight.terminal_origin) {
      changes.push({
        type: 'terminal_change',
        title: `✈️ ${flightIdent} — Terminal changed to ${flight.terminal_origin}`,
        body: `Departure terminal changed to ${flight.terminal_origin}. Was ${prev.terminal_origin}.`,
      });
    }

    // Check arrival terminal change
    if (prev.terminal_destination && flight.terminal_destination && prev.terminal_destination !== flight.terminal_destination) {
      changes.push({
        type: 'terminal_change',
        title: `✈️ ${flightIdent} — Arrival terminal changed to ${flight.terminal_destination}`,
        body: `Arrival terminal changed to ${flight.terminal_destination}. Was ${prev.terminal_destination}.`,
      });
    }

    // Check baggage carousel assigned
    if (!prev.baggage_claim && flight.baggage_claim) {
      changes.push({
        type: 'baggage',
        title: `✈️ ${flightIdent} — Baggage at Carousel ${flight.baggage_claim}`,
        body: `${flightIdent} — Landed. Baggage at Carousel ${flight.baggage_claim}.`,
      });
    }

    // Update stored state
    flightStore.set(flightId, {
      status: currentStatus,
      delay: currentDelay,
      gate_origin: flight.gate_origin,
      gate_destination: flight.gate_destination,
      terminal_origin: flight.terminal_origin,
      terminal_destination: flight.terminal_destination,
      baggage_claim: flight.baggage_claim,
    });

    // Send notifications for changes
    if (changes.length > 0) {
      for (const [fcmToken, device] of deviceStore) {
        if (device.trackedFlights.has(flightId)) {
          for (const change of changes) {
            await sendNotification(fcmToken, flightId, change.title, change.body, change.type);
          }
        }
      }
    }

    // Small delay between API calls to avoid rate limiting
    await new Promise(resolve => setTimeout(resolve, 500));
  }
}

// Poll every 2 minutes
cron.schedule('*/2 * * * *', () => {
  console.log(`[${new Date().toISOString()}] Starting flight check...`);
  checkFlightUpdates().catch(err => console.error('Flight check error:', err));
});

// Start server
app.listen(PORT, () => {
  console.log(`Flight Tracker Backend running on port ${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/health`);
});
