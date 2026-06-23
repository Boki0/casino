import './Footer.css'

function Footer() {
  return (
    <footer className="site-footer">
      <div className="site-footer__container">
        <div className="site-footer__top">
          <div className="site-footer__columns">
            <div className="site-footer__column">
              <h2 className="site-footer__title">Main</h2>
              <a className="site-footer__item" href="#">
                Games
              </a>
            </div>

            <div className="site-footer__column">
              <h2 className="site-footer__title">Team</h2>
              <a className="site-footer__item" href="#">
                About
              </a>
            </div>

            <div className="site-footer__column">
              <h2 className="site-footer__title">Info</h2>
            </div>

            <div className="site-footer__column">
              <h2 className="site-footer__title">Profile</h2>
              <a className="site-footer__item" href="#">
                Deposit
              </a>
              <a className="site-footer__item" href="#">
                Withdraw
              </a>
            </div>
          </div>

          <div className="site-footer__support">
            <div className="site-footer__support-icon">🎧</div>
            <div>
              <div className="site-footer__support-heading">
                <h2 className="site-footer__title">Support</h2>
                <span>24/7</span>
              </div>
              <p>Contact: bojan.vasic01@gmail.com</p>
              <span className="site-footer__support-button">Write Us</span>
            </div>
          </div>
        </div>

        <div className="site-footer__brand-row">
          <div className="site-footer__brand">
            <div className="site-footer__logo">♠</div>
            <div>
              <strong>Casino Platform</strong>
              <p>© 2026 Casino Platform | All Rights Reserved.</p>
            </div>
          </div>
        </div>

        <div className="site-footer__bottom">
          <div className="site-footer__contacts">
            <span>Support: bojan.vasic01@gmail.com</span>
            <span>Partners: bojan.vasic01@gmail.com</span>
            <span>Press: bojan.vasic01@gmail.com</span>
          </div>
        </div>
      </div>
    </footer>
  )
}

export default Footer
