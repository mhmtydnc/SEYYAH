import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Link, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import 'leaflet/dist/leaflet.css'
import { OturumSaglayici, useOturum } from './oturum/Oturum'
import { AnaSayfa } from './sayfalar/AnaSayfa'
import { KimlikSayfasi } from './sayfalar/KimlikSayfasi'
import { RotalarimSayfasi } from './sayfalar/RotalarimSayfasi'
import { YolculukSayfasi } from './sayfalar/YolculukSayfasi'
import './stil.css'

function UstCubuk() {
  const { kullanici, hazir, cikisYap } = useOturum()
  const yonlendir = useNavigate()
  const konum = useLocation()
  if (konum.pathname === '/yolculuk') return null
  return <header className="ust-cubuk"><div className="ust-icerik">
    <Link className="logo" to="/">Seyyah<span>.</span></Link>
    <nav aria-label="Ana menü">{hazir && (kullanici ? <>
      <span className="kullanici-adi">{kullanici.ad}</span><Link to="/rotalarim">Rotalarım</Link>
      <button type="button" onClick={() => { cikisYap(); yonlendir('/') }}>Çıkış</button>
    </> : <><Link to="/giris">Giriş</Link><Link className="kayit-baglanti" to="/kayit">Kayıt ol</Link></>)}</nav>
  </div></header>
}

function Uygulama() {
  return <OturumSaglayici><BrowserRouter>
    <UstCubuk />
    <Routes>
      <Route path="/" element={<AnaSayfa />} />
      <Route path="/giris" element={<KimlikSayfasi key="giris" tur="giris" />} />
      <Route path="/kayit" element={<KimlikSayfasi key="kayit" tur="kayit" />} />
      <Route path="/rotalarim" element={<RotalarimSayfasi />} />
      <Route path="/yolculuk" element={<YolculukSayfasi />} />
    </Routes>
  </BrowserRouter></OturumSaglayici>
}

ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><Uygulama /></React.StrictMode>)
