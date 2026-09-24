import { describe, expect, it } from 'vitest'
import { leafletKoordinatlari } from './koordinat'

describe('leafletKoordinatlari', () => {
  it('GeoJSON boylam-enlem sırasını Leaflet enlem-boylam sırasına çevirir', () => {
    expect(leafletKoordinatlari([[29.01, 41], [34.83, 38.64]])).toEqual([[41, 29.01], [38.64, 34.83]])
  })

  it('boş geometriyi boş bırakır', () => {
    expect(leafletKoordinatlari([])).toEqual([])
  })
})
