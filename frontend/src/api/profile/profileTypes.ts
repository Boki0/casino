export type UserProfile = {
  id: string
  authUserId: string
  email: string
  username: string
  displayName: string | null
  firstName: string | null
  lastName: string | null
  country: string | null
  phoneNumber: string | null
  dateOfBirth: string | null
  avatarUrl: string | null
  refCode: string
  createdAt: string
  updatedAt: string
}
