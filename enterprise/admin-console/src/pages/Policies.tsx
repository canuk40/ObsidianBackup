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
  MenuItem,
  Chip,
} from '@mui/material'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { Add as AddIcon } from '@mui/icons-material'
import { policyService } from '../services/api'

const policyTypes = [
  'BACKUP_SCHEDULE',
  'RETENTION',
  'ENCRYPTION',
  'NETWORK',
  'COMPLIANCE',
]

export default function Policies() {
  const [policies, setPolicies] = useState([])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [formData, setFormData] = useState({
    name: '',
    type: '',
    config: {},
    targetDevices: [],
  })

  useEffect(() => {
    loadPolicies()
  }, [])

  const loadPolicies = async () => {
    try {
      const data = await policyService.getAll()
      setPolicies(data)
    } catch (error) {
      console.error('Failed to load policies:', error)
    }
  }

  const handleSave = async () => {
    try {
      await policyService.create(formData)
      setDialogOpen(false)
      setFormData({ name: '', type: '', config: {}, targetDevices: [] })
      loadPolicies()
    } catch (error) {
      console.error('Failed to create policy:', error)
    }
  }

  const handleEnforce = async (id: string) => {
    try {
      await policyService.enforce(id)
      loadPolicies()
    } catch (error) {
      console.error('Failed to enforce policy:', error)
    }
  }

  const columns: GridColDef[] = [
    { field: 'name', headerName: 'Policy Name', width: 250 },
    { field: 'type', headerName: 'Type', width: 180 },
    {
      field: 'isEnforced',
      headerName: 'Status',
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value ? 'Enforced' : 'Inactive'}
          color={params.value ? 'success' : 'default'}
          size="small"
        />
      ),
    },
    {
      field: 'targetDevices',
      headerName: 'Target Devices',
      width: 150,
      renderCell: (params) => params.value.length,
    },
    { field: 'updatedAt', headerName: 'Last Updated', width: 200 },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 200,
      renderCell: (params) => (
        <Box>
          <Button
            size="small"
            onClick={() => handleEnforce(params.row.id)}
            disabled={params.row.isEnforced}
          >
            Enforce
          </Button>
          <Button size="small" color="error">
            Delete
          </Button>
        </Box>
      ),
    },
  ]

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h4">Policy Management</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setDialogOpen(true)}
        >
          Create Policy
        </Button>
      </Box>

      <Box sx={{ height: 600, width: '100%' }}>
        <DataGrid
          rows={policies}
          columns={columns}
          pageSize={10}
          rowsPerPageOptions={[10, 25, 50]}
        />
      </Box>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Create New Policy</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Policy Name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            margin="normal"
          />
          <TextField
            fullWidth
            select
            label="Policy Type"
            value={formData.type}
            onChange={(e) => setFormData({ ...formData, type: e.target.value })}
            margin="normal"
          >
            {policyTypes.map((type) => (
              <MenuItem key={type} value={type}>
                {type}
              </MenuItem>
            ))}
          </TextField>
          <Typography variant="body2" color="textSecondary" sx={{ mt: 2 }}>
            Configure policy settings based on the selected type.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleSave} variant="contained">
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
