import { useEffect, useState } from 'react'
import {
  Box,
  Typography,
  Grid,
  Paper,
  Button,
} from '@mui/material'
import { Download as DownloadIcon } from '@mui/icons-material'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { reportService } from '../services/api'

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042']

export default function Reports() {
  const [storageUsage, setStorageUsage] = useState<any[]>([])
  const [compliance, setCompliance] = useState<any>(null)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      const [storage, comp] = await Promise.all([
        reportService.getStorageUsage(),
        reportService.getCompliance()
      ])
      
      setStorageUsage(
        Object.entries(storage).map(([name, size]) => ({
          name,
          size: Number(size) / (1024 * 1024 * 1024) // Convert to GB
        }))
      )
      
      setCompliance(comp)
    } catch (error) {
      console.error('Failed to load reports:', error)
    }
  }

  const complianceData = compliance ? [
    { name: 'Compliant', value: compliance.compliantDevices },
    { name: 'Non-Compliant', value: compliance.nonCompliantDevices }
  ] : []

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Reports & Analytics</Typography>
        <Button
          variant="contained"
          startIcon={<DownloadIcon />}
        >
          Export PDF Report
        </Button>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Storage Usage by Device (GB)
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={storageUsage}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="size" fill="#1976d2" />
              </BarChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Compliance Status
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={complianceData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={(entry) => entry.name}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {complianceData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
            {compliance && (
              <Box sx={{ mt: 2, textAlign: 'center' }}>
                <Typography variant="h5">
                  {(compliance.complianceRate * 100).toFixed(1)}%
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  Overall Compliance Rate
                </Typography>
              </Box>
            )}
          </Paper>
        </Grid>

        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              GDPR & HIPAA Compliance Summary
            </Typography>
            <Box sx={{ mt: 2 }}>
              <Typography variant="body1" gutterBottom>
                ✓ All backup data encrypted at rest and in transit
              </Typography>
              <Typography variant="body1" gutterBottom>
                ✓ Audit logs maintained for all data access
              </Typography>
              <Typography variant="body1" gutterBottom>
                ✓ Data retention policies enforced
              </Typography>
              <Typography variant="body1" gutterBottom>
                ✓ User consent tracked for data processing
              </Typography>
              <Typography variant="body1" gutterBottom>
                ✓ Right to erasure (remote wipe) available
              </Typography>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}
