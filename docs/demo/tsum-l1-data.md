# TSUM L1 demo data track

## Status and provenance

This dataset is **demo-only and pending an on-site survey and facility-owner validation**. It must
not be treated as an as-built accessibility or life-safety map.

- Source title: `TSUM_FP_3.1.1_Floor-plan_L1`.
- Source sheet: A3, 1191 x 842 PDF points, one page, plan version 02 / sheet 3.1.1,
  dated 2026-07-16.
- Source SHA-256: `DDE65CDAE099D6B0B361DD7EC16291529E527D3352B9CF42F9E275F253D8F2B6`.
- The source PDF is intentionally not stored in Git.

## Map crop and coordinate transform

The web map is `src/main/resources/static/demo/maps/tsum-l1.png`, an optimized indexed-color PNG
at 2048 x 1712 pixels. The PDF uses a bottom-left origin; Kivo uses a top-left origin.

- PDF crop box: left `105`, bottom `140`, right `775`, top `700` points.
- Crop size: `670 x 560` points.
- Kivo coordinate size: `10000 x 8358`.
- Uniform scale: `10000 / 670 = 14.9253731343` coordinate units per PDF point.

Forward transform, before rounding to the nearest integer:

```text
kivoX = (pdfX - 105) * 10000 / 670
kivoY = (700 - pdfY) * 10000 / 670
```

Inverse transform:

```text
pdfX = 105 + kivoX * 670 / 10000
pdfY = 700 - kivoY * 670 / 10000
```

The PNG raster uses the same crop and aspect ratio. Overlay pixels are therefore:

```text
pixelX = kivoX * 2048 / 10000
pixelY = kivoY * 1712 / 8358
```

The one-pixel height rounding is intentional: `round(2048 * 8358 / 10000) = 1712`.

## Interpretation of the plan

The plan legend distinguishes workshops, exhibition/fair areas, media library, educational and
congress areas, offices, gastronomy, kitchens, public areas, reserve rooms, service rooms, and
technical rooms. The sheet also marks information, ATM/vending, charging, prayer room,
mother-and-child room, children's area, medical room, accessible facilities, cloakroom, toilets,
smoking area, lifts, stairs, escalators, parking and entrances.

The routable demo anchors cover the gallery, cloakroom, two exhibition spaces, three restaurant
zones, the Kazan excursion center, culinary workshops, and selected named workshops: Aybat,
Bikmatur, Tatardar, Kazan Ceramics, Artel Masters, Tugannar, and Kazan Silver. External callout
leaders are not precise survey points, so every workshop anchor ends at an approximate public
door/corridor position rather than inside a room.

The graph follows approximate centerlines of white public circulation and open exhibition/atrium
areas. It deliberately excludes offices, kitchens, reserve rooms, service rooms, server/technical
rooms, storage, and back-of-house toilet interiors. Examples visible on the sheet but excluded are
offices B102/B103/B105, service rooms 106.1/113.1/127.1, server room C1.2, reserve rooms 112.1,
114.1, 115.1, B106, B107, 120.1, 121.1 and 128, plus kitchen 131.

## Entrances, vertical circulation, and accessibility

The single demo kiosk is placed just inside the conspicuous south public vestibule, near the
bottom-center entrance doors on the cropped map. Other exterior door banks are visible, but their
public status and operating schedule are not confirmed, so they do not create additional kiosk
starts.

Lifts, stairs, and the central escalator are visible on L1. No vertical edge is included because no
other-floor plan or verified landing pairing was supplied. Consequently the demo cannot claim a
wheelchair-accessible inter-floor route. Same-floor edges are marked wheelchair-accessible only
for the drawn open public circulation used by this graph; door clearances, thresholds, gradients,
temporary fixtures, and operating restrictions still require on-site validation. No route enters a
service, technical, kitchen, office, or reserve area.

## Metric and topology assumptions

- `meters_per_unit = 0.01` is a provisional ETA scale, not a surveyed calibration.
- Edge lengths are Euclidean lengths of the demo centerlines multiplied by that provisional scale.
- Doors and tenant boundaries are interpreted visually from the PDF; no CAD layer semantics were
  available to the importer.
- Named workshop-to-room associations are inferred from leader lines and remain provisional.
- Temporary walls, queues, furniture, exhibits, closures, fire-compartment rules, and one-way
  operating constraints are unknown.
- The published graph is bidirectional because the sheet does not establish public one-way flow.

Before production use, survey coordinates and distances, confirm every public entrance and door,
validate all active kiosk positions, map every vertical landing across floors, and have the facility
owner approve accessibility and restricted-area boundaries.
