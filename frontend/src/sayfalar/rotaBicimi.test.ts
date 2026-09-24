import { describe, expect, it } from 'vitest'
import { mesafeFarkiMetni, sureFarkiMetni } from './rotaBicimi'

describe('alternatif rota farkları', () => {
  it('saat ve dakika farkını biçimlendirir', () => {
    expect(sureFarkiMetni(3900)).toBe('+1 sa 5 dk')
    expect(sureFarkiMetni(1500)).toBe('+25 dk')
  })

  it('kilometre farkını biçimlendirir', () => {
    expect(mesafeFarkiMetni(34800)).toBe('+34 km')
  })

  it('negatif ve sıfır farkları göstermez', () => {
    expect(sureFarkiMetni(0)).toBeNull()
    expect(sureFarkiMetni(-60)).toBeNull()
    expect(mesafeFarkiMetni(0)).toBeNull()
    expect(mesafeFarkiMetni(-1000)).toBeNull()
  })
})
