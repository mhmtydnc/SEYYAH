import { describe, expect, it } from 'vitest'
import { kategoriAdi } from './kategoriler'

describe('kategoriAdi', () => {
  it('bilinen OSM kategorilerini Türkçe gösterir', () => {
    expect(kategoriAdi('attraction')).toBe('Turistik yer')
    expect(kategoriAdi('place_of_worship')).toBe('İbadet yeri')
  })

  it('bilinmeyen kategoriyi değiştirmeden döndürür', () => {
    expect(kategoriAdi('yeni_kategori')).toBe('yeni_kategori')
  })
})
