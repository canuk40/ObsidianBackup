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
  Grid,
  Paper,
  List,
  ListItem,
  ListItemText,
  Checkbox,
  FormGroup,
  FormControlLabel,
} from '@mui/material'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { Add as AddIcon } from '@mui/icons-material'
import { rbacService } from '../services/api'

export default function RBAC() {
  const [roles, setRoles] = useState([])
  const [permissions, setPermissions] = useState([])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    permissions: [] as string[],
  })

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      const [rolesData, permsData] = await Promise.all([
        rbacService.getRoles(),
        rbacService.getPermissions()
      ])
      setRoles(rolesData)
      setPermissions(permsData)
    } catch (error) {
      console.error('Failed to load RBAC data:', error)
    }
  }

  const handleSave = async () => {
    try {
      await rbacService.createRole(formData)
      setDialogOpen(false)
      setFormData({ name: '', description: '', permissions: [] })
      loadData()
    } catch (error) {
      console.error('Failed to create role:', error)
    }
  }

  const handlePermissionToggle = (permission: string) => {
    setFormData(prev => ({
      ...prev,
      permissions: prev.permissions.includes(permission)
        ? prev.permissions.filter(p => p !== permission)
        : [...prev.permissions, permission]
    }))
  }

  const columns: GridColDef[] = [
    { field: 'name', headerName: 'Role Name', width: 200 },
    { field: 'description', headerName: 'Description', width: 300 },
    {
      field: 'permissions',
      headerName: 'Permissions',
      width: 400,
      renderCell: (params) => (
        <Box>
          {params.value.slice(0, 3).map((perm: string) => (
            <Chip key={perm} label={perm} size="small" sx={{ mr: 0.5 }} />
          ))}
          {params.value.length > 3 && (
            <Chip label={`+${params.value.length - 3} more`} size="small" />
          )}
        </Box>
      ),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 150,
      renderCell: (params) => (
        <Button size="small" color="error">
          Delete
        </Button>
      ),
    },
  ]

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h4">Role-Based Access Control</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setDialogOpen(true)}
        >
          Create Role
        </Button>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Roles
            </Typography>
            <Box sx={{ height: 400, width: '100%' }}>
              <DataGrid
                rows={roles}
                columns={columns}
                pageSize={10}
                rowsPerPageOptions={[10, 25, 50]}
              />
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Available Permissions
            </Typography>
            <List dense>
              {permissions.map((perm) => (
                <ListItem key={perm}>
                  <ListItemText primary={perm} />
                </ListItem>
              ))}
            </List>
          </Paper>
        </Grid>
      </Grid>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Create New Role</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Role Name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            margin="normal"
          />
          <TextField
            fullWidth
            label="Description"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            margin="normal"
            multiline
            rows={2}
          />
          <Typography variant="subtitle1" sx={{ mt: 2, mb: 1 }}>
            Permissions
          </Typography>
          <FormGroup>
            {permissions.map((perm) => (
              <FormControlLabel
                key={perm}
                control={
                  <Checkbox
                    checked={formData.permissions.includes(perm)}
                    onChange={() => handlePermissionToggle(perm)}
                  />
                }
                label={perm}
              />
            ))}
          </FormGroup>
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
