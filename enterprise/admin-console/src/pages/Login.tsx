import { useState } from 'react'
import { Box, TextField, Button, Typography, Paper, Container } from '@mui/material'
import { authService } from '../services/api'

interface LoginProps {
  onLogin: () => void
}

export default function Login({ onLogin }: LoginProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    
    try {
      await authService.login(email, password)
      onLogin()
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed')
    }
  }

  return (
    <Container maxWidth="sm">
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Paper elevation={3} sx={{ p: 4, width: '100%' }}>
          <Typography component="h1" variant="h4" align="center" gutterBottom>
            ObsidianBackup Enterprise
          </Typography>
          <Typography variant="body2" align="center" color="text.secondary" sx={{ mb: 3 }}>
            Admin Console
          </Typography>
          
          <Box component="form" onSubmit={handleSubmit}>
            <TextField
              margin="normal"
              required
              fullWidth
              label="Email Address"
              autoComplete="email"
              autoFocus
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              label="Password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            
            {error && (
              <Typography color="error" variant="body2" sx={{ mt: 2 }}>
                {error}
              </Typography>
            )}
            
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
            >
              Sign In
            </Button>
            
            <Button
              fullWidth
              variant="outlined"
              onClick={() => {/* SSO login */}}
            >
              Sign In with SSO
            </Button>
          </Box>
        </Paper>
      </Box>
    </Container>
  )
}
