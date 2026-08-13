import { useAuth } from '../auth/useAuth'
import './ProfilePage.css'

function ProfilePage() {
  const { profile } = useAuth()

  return (
    <section className="profile-page">
      <div className="profile-card">
        <p className="profile-card__eyebrow">Player profile</p>
        <h1>{profile?.username ?? 'Profile'}</h1>
        {profile ? (
          <dl className="profile-card__details">
            <div>
              <dt>Username</dt>
              <dd>{profile.username}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{profile.email}</dd>
            </div>
          </dl>
        ) : (
          <p className="profile-card__status">Profile information is not available.</p>
        )}
      </div>
    </section>
  )
}

export default ProfilePage
