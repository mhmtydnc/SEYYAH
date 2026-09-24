import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useOturum } from '../oturum/Oturum'

export function KimlikSayfasi({ tur }: { tur: 'giris' | 'kayit' }) {
  const { girisYap, kayitOl } = useOturum()
  const yonlendir = useNavigate()
  const [ad, adAyarla] = useState('')
  const [eposta, epostaAyarla] = useState('')
  const [sifre, sifreAyarla] = useState('')
  const [hata, hataAyarla] = useState('')
  const [bekliyor, bekliyorAyarla] = useState(false)

  async function gonder(event: FormEvent) {
    event.preventDefault()
    hataAyarla('')
    if (tur === 'kayit' && !ad.trim()) { hataAyarla('Adınızı girin.'); return }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(eposta.trim())) { hataAyarla('Geçerli bir e-posta adresi girin.'); return }
    if (sifre.length < 8) { hataAyarla('Şifre en az 8 karakter olmalı.'); return }
    bekliyorAyarla(true)
    try {
      if (tur === 'kayit') await kayitOl(ad.trim(), eposta.trim(), sifre)
      else await girisYap(eposta.trim(), sifre)
      yonlendir('/')
    } catch (neden) {
      hataAyarla(neden instanceof Error ? neden.message : 'İşlem tamamlanamadı.')
    } finally {
      bekliyorAyarla(false)
    }
  }

  const kayit = tur === 'kayit'
  return <main className="kimlik-sayfasi">
    <div className="kimlik-karti">
      <p className="ust-etiket">SEYYAH</p>
      <h1>{kayit ? 'Yolculuğa katıl' : 'Tekrar hoş geldin'}</h1>
      <p>{kayit ? 'Rotalarını kaydetmek için hesap oluştur.' : 'Kayıtlı rotalarına ulaşmak için giriş yap.'}</p>
      <form onSubmit={(event) => void gonder(event)} noValidate>
        {kayit && <label>Ad<input value={ad} onChange={(event) => adAyarla(event.target.value)} autoComplete="name" required /></label>}
        <label>E-posta<input type="email" value={eposta} onChange={(event) => epostaAyarla(event.target.value)} autoComplete="email" required /></label>
        <label>Şifre<input type="password" value={sifre} onChange={(event) => sifreAyarla(event.target.value)}
          autoComplete={kayit ? 'new-password' : 'current-password'} minLength={8} required /></label>
        {hata && <div className="uyari hata" role="alert">{hata}</div>}
        <button type="submit" className="birincil" disabled={bekliyor}>{bekliyor ? 'Lütfen bekleyin…' : kayit ? 'Kayıt ol' : 'Giriş yap'}</button>
      </form>
      <p className="kimlik-gecis">{kayit ? 'Zaten hesabın var mı?' : 'Henüz hesabın yok mu?'} <Link to={kayit ? '/giris' : '/kayit'}>{kayit ? 'Giriş yap' : 'Kayıt ol'}</Link></p>
    </div>
  </main>
}
