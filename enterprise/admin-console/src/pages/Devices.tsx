import { useEffect, useState } from 'react'
import {
  Box,
  Typography,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Chip,
} from '@mui/material'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { Delete as DeleteIcon } from '@mui/icons-material'
import { deviceService } from '../services/api'

export default function Devices() {
  const [devices, setDevices] = useState([])
  const [wipeDialogOpen, setWipeDialogOpen] = useState(false)
  const [selectedDevice, setSelectedDevice] = useState<any>(null)
  const [wipeReason, setWipeReason] = useState('')

  useEffect(() => {
    loadDevices()
  }, [])

  const loadDevices = async () => {
    try {
      const data = await deviceService.getAll()
      setDevices(data)
    } catch (error) {
      console.error('Failed to load devices:', error)
    }
  }

  const handleWipe = async () => {
    if (!selectedDevice) return
    
    try {
      await deviceService.wipe(selectedDevice.id, wipeReason)
      setWipeDialogOpen(false)
      setWipeReason('')
      loadDevices()
    } catch (error) {
      console.error('Failed to wipe device:', error)
    }
  }

  const columns: GridColDef[] = [
    { field: 'name', headerName: 'Device Name', width: 200 },
    { field: 'platform', headerName: 'Platform', width: 120 },
    { field: 'osVersion', headerName: 'OS Version', width: 120 },
    { field: 'appVersion', headerName: 'App Version', width: 120 },
    {
      field: 'status',
      headerName: 'Status',
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value}
          color={params.value === 'ACTIVE' ? 'success' : 'default'}
          size="small"
        />
      ),
    },
    {
      field: 'complianceStatus',
      headerName: 'Compliance',
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value.isCompliant ? 'Compliant' : 'Non-Compliant'}
          color={params.value.isCompliant ? 'success' : 'error'}
          size="small"
        />
      ),
    },
    { field: 'lastSyncAt', headerName: 'Last Sync', width: 200 },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 150,
      renderCell: (params) => (
        <Button
          variant="outlined"
          color="error"
          size="small"
          startIcon={<DeleteIcon />}
          onClick={() => {
            setSelectedDevice(params.row)
            setWipeDialogOpen(true)
          }}
        >
          Wipe
        </Button>
      ),
    },
  ]

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Device Management
      </Typography>

      <Box sx={{ height: 600, width: '100%' }}>
        <DataGrid
          rows={devices}
          columns={columns}
          pageSize={10}
          rowsPerPageOptions={[10, 25, 50]}
          checkboxSelection
          disableSelectionOnClick
        />
      </Box>

      <Dialog open={wipeDialogOpen} onClose={() => setWipeDialogOpen(false)}>
        <DialogTitle>Confirm Device Wipe</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            Are you sure you want to wipe device: {selectedDevice?.name}?
          </Typography>
          <Typography variant="body2" color="error" gutterBottom>
            This action cannot be undone. All data on the device will be erased.
          </Typography>
          <TextField
            fullWidth
            label="Reason for wiping"
            value={wipeReason}
            onChange={(e) => setWipeReason(e.target.value)}
            multiline
            rows={3}
            margin="normal"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setWipeDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleWipe} color="error" variant="contained">
            Wipe Device
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
