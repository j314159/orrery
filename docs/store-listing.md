# Orrery — Play Store listing content

Paste-ready content for the Play Console. Character limits noted where they apply.

## App name (30 chars max)

> Orrery: Watch Sky Map

(21 chars. Alternative if you want plainer: `Orrery — Planets Overhead`, 25 chars.)

## Short description (80 chars max)

> See which planets are overhead right now — a live sky map on your watch.

(73 chars)

## Full description (4,000 chars max)

> Orrery turns your Wear OS watch into a pocket planetarium. Glance at your wrist to see exactly which planets, stars, and constellations are above you right now — computed for your actual location, entirely on your watch.
>
> **A live map of your sky**
> The circular sky map shows the whole visible sky at once: the horizon around the edge, the zenith at the center, and every planet, the Sun, and the Moon placed exactly where they are for your location. The background shifts through sunrise, daylight, twilight, and night to match the real sky above you.
>
> **Planets, stars, and constellations**
> • All the planets — or just the naked-eye ones, your choice
> • The Moon with its real current phase
> • The 170 brightest stars, sized by brightness
> • 25 constellations with stick figures and names
> • The ecliptic — the road the planets travel across the sky
>
> **Scrub through time with the crown**
> Turn the watch crown to roll the sky forward or backward an hour at a time. See where Jupiter will be at midnight, or when the Moon sets. Tap once to snap back to now.
>
> **Watch face complication**
> Add the "Planets overhead" complication to your watch face to see at a glance how many planets are up, updated automatically through the day.
>
> **Private by design**
> Orrery works completely offline. Your location is used only on the watch to compute the sky — it is never sent anywhere. No ads, no analytics, no accounts, no tracking. Free, with nothing to buy.
>
> Made by one person who wanted to know what that bright dot above the sunset was. (It was Venus.)

## Release notes for v1.1.0 (500 chars max)

> • Better location handling: clearer prompts, retry on errors, and recovery when permission is denied
> • Now uses approximate location only — precise location is never requested
> • Fixed the complication counting the Moon as a planet
> • Correct moon phase orientation in the southern hemisphere
> • Stability fixes for the watch face complication

## Assets (all in the project root / screenshots/)

| Asset | File | Spec |
|---|---|---|
| App icon | `store-icon-512-square.png` | 512×512 ✓ |
| Feature graphic | `store-feature-graphic.png` or `store-feature-graphic-polish-1024x500.png` | 1024×500 ✓ |
| Phone screenshots (min 2) | `screenshots/phone-01.png` … `phone-06.png` | 1080×1920 ✓ |
| Wear screenshots | `screenshots/wear-01.png` … `wear-06.png` | 454×454, 1:1 ✓ |

## Play Console questionnaire answers

- **App or game:** App. **Category:** Tools (or Books & Reference). **Free.**
- **Privacy policy URL:** required — the page in `docs/privacy-policy.html`, once hosted.
- **Data safety:** location is processed on-device only and never transmitted, which under
  Play's definitions means you can answer **"No data collected"** and **"No data shared."**
  (Their definition of "collected" = transmitted off the device.)
- **Ads:** No. **App access:** all functionality available without special access (no login).
- **Content rating questionnaire:** category Utility; answer No to everything → rated Everyone.
- **Target audience:** 13 and older (selecting under-13 triggers extra "Families" requirements
  — not worth it).
- **News app:** No. **COVID-19 app:** No. **Government app:** No.
- **Form factors:** opt in to **Wear OS** (Release → Advanced settings → Form factors).
  The Wear release gets an extra Google review against Wear OS quality guidelines.
- **Countries:** all available.

## Upload artifact

`app/build/outputs/bundle/release/app-release.aab` — v1.1.0 (versionCode 4), signed.
Enroll in **Play App Signing** when prompted at first upload (Google escrows the signing
key; your local keystore becomes the upload key — this protects the app if the keystore
is ever lost).
