import { describe, expect, it } from 'vitest'
import { durum } from './acilisSaati'

const tarih = (gun: number, saat: number, dakika = 0) => new Date(2026, 8, gun, saat, dakika)

describe('açılış saati durumu', () => {
  it('her gün açık, kapalı ve kalışta kapanacak durumları ayırır', () => {
    expect(durum('Mo-Su 09:00-17:00', tarih(21, 10), 60)).toEqual({ durum: 'acik', saat: null })
    expect(durum('Mo-Su 09:00-17:00', tarih(21, 8), 60)).toEqual({ durum: 'kapali', saat: '09:00' })
    expect(durum('Mo-Su 09:00-17:00', tarih(21, 16, 30), 60)).toEqual({ durum: 'kalisSirasindaKapaniyor', saat: '17:00' })
  })

  it('pazartesi kapalı kuralını ve ertesi günün açılışını kullanır', () => {
    expect(durum('Tu-Su 09:00-17:00', tarih(21, 12), 60)).toEqual({ durum: 'kapali', saat: '09:00' })
    expect(durum('Tu-Su 09:00-17:00', tarih(22, 10), 60)).toEqual({ durum: 'acik', saat: null })
    expect(durum('Mo-Su 09:00-17:00', tarih(21, 23), 60)).toEqual({ durum: 'kapali', saat: '09:00' })
  })

  it('24/7 ve çözülemeyen metinde uyarı üretmez', () => {
    expect(durum('24/7', tarih(21, 22), 300)).toEqual({ durum: 'acik', saat: null })
    expect(durum('rastgele metin', tarih(21, 10), 60)).toEqual({ durum: 'bilinmiyor', saat: null })
    expect(durum(null, tarih(21, 10), 60)).toEqual({ durum: 'bilinmiyor', saat: null })
  })
})
