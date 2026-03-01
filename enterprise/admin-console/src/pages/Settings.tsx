import { Box, Typography, Paper, Grid, TextField, Button } from '@mui/material'

export default function Settings() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Enterprise Settings
      </Typography>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Organization Settings
            </Typography>
            <TextField fullWidth label="Organization Name" margin="normal" />
            <TextField fullWidth label="Domain" margin="normal" />
            <TextField fullWidth label="Max Devices" type="number" margin="normal" />
            <Button variant="contained" sx={{ mt: 2 }}>
              Save Changes
            </Button>
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              SAML/SSO Configuration
            </Typography>
            <TextField fullWidth label="Entity ID" margin="normal" />
            <TextField fullWidth label="SSO URL" margin="normal" />
            <TextField fullWidth label="Certificate" multiline rows={4} margin="normal" />
            <Button variant="contained" sx={{ mt: 2 }}>
              Update SSO
            </Button>
          </Paper>
        </Grid>

        <Grid item xs={12}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Compliance Settings
            </Typography>
            <Typography variant="body2" color="textSecondary" paragraph>
              Configure GDPR and HIPAA compliance settings for your organization.
            </Typography>
            <TextField fullWidth label="Data Retention Period (days)" type="number" margin="normal" />
            <TextField fullWidth label="Backup Encryption Standard" margin="normal" defaultValue="AES-256" />
            <TextField fullWidth label="Audit Log Retention (days)" type="number" margin="normal" />
            <Button variant="contained" sx={{ mt: 2 }}>
              Save Compliance Settings
            </Button>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}
