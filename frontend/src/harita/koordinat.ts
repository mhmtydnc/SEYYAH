import type { LatLngTuple } from 'leaflet'

export function leafletKoordinatlari(geometri: [number, number][]): LatLngTuple[] {
  return geometri.map(([boylam, enlem]) => [enlem, boylam])
}
