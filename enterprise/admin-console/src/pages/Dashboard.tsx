import { useEffect, useState } from 'react'
import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
} from '@mui/material'
import {
  Devices as DevicesIcon,
  CloudDone as BackupIcon,
  Storage as StorageIcon,
  VerifiedUser as ComplianceIcon,
} from '@mui/icons-material'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { reportService } from '../services/api'

export default function Dashboard() {
  const [stats, setStats] = useState<any>(null)
  const [successRates, setSuccessRates] = useState<any[]>([])

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      const [dashboardStats, backupRates] = await Promise.all([
        reportService.getDashboard(),
        reportService.getBackupSuccessRates(30)
      ])
      setStats(dashboardStats)
      setSuccessRates(backupRates)
    } catch (error) {
      console.error('Failed to load dashboard data:', error)
    }
  }

  const StatCard = ({ title, value, icon, color }: any) => (
    <Card>
      <CardContent>
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Box>
            <Typography color="textSecondary" gutterBottom variant="body2">
              {title}
            </Typography>
            <Typography variant="h4">{value}</Typography>
          </Box>
          <Box sx={{ color, fontSize: 40 }}>
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  )

  if (!stats) return <Typography>Loading...</Typography>

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Devices"
            value={stats.totalDevices}
            icon={<DevicesIcon />}
            color="#1976d2"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Active Devices"
            value={stats.activeDevices}
            icon={<DevicesIcon />}
            color="#2e7d32"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Successful Backups"
            value={stats.successfulBackups}
            icon={<BackupIcon />}
            color="#2e7d32"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Compliance Rate"
            value={`${(stats.complianceRate * 100).toFixed(1)}%`}
            icon={<ComplianceIcon />}
            color="#ed6c02"
          />
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Backup Success Rate (Last 30 Days)
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={successRates}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="successRate" stroke="#2e7d32" name="Success Rate" />
              </LineChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Storage Usage
            </Typography>
            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" color="textSecondary">
                Total Storage Used
              </Typography>
              <Typography variant="h5">
                {(stats.totalStorageUsed / (1024 * 1024 * 1024)).toFixed(2)} GB
              </Typography>
              <Typography variant="body2" color="textSecondary" sx={{ mt: 2 }}>
                Average Backup Size
              </Typography>
              <Typography variant="h6">
                {(stats.averageBackupSize / (1024 * 1024)).toFixed(2)} MB
              </Typography>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}
