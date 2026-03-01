import { useEffect, useState } from 'react'
import { Box, Typography, Button, TextField } from '@mui/material'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { Download as DownloadIcon } from '@mui/icons-material'
import { DatePicker } from '@mui/x-date-pickers/DatePicker'
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider'
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns'
import { auditService } from '../services/api'

export default function AuditLogs() {
  const [logs, setLogs] = useState([])
  const [startDate, setStartDate] = useState<Date | null>(null)
  const [endDate, setEndDate] = useState<Date | null>(null)
  const [page, setPage] = useState(1)

  useEffect(() => {
    loadLogs()
  }, [page])

  const loadLogs = async () => {
    try {
      const params: any = { page, pageSize: 50 }
      if (startDate) params.startDate = startDate.toISOString()
      if (endDate) params.endDate = endDate.toISOString()
      
      const data = await auditService.query(params)
      setLogs(data)
    } catch (error) {
      console.error('Failed to load audit logs:', error)
    }
  }

  const handleExport = async () => {
    try {
      const params: any = {}
      if (startDate) params.startDate = startDate.toISOString()
      if (endDate) params.endDate = endDate.toISOString()
      
      const blob = await auditService.export(params)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'audit-logs.csv'
      a.click()
    } catch (error) {
      console.error('Failed to export logs:', error)
    }
  }

  const columns: GridColDef[] = [
    { field: 'timestamp', headerName: 'Timestamp', width: 200 },
    { field: 'userId', headerName: 'User ID', width: 150 },
    { field: 'action', headerName: 'Action', width: 180 },
    { field: 'resourceType', headerName: 'Resource Type', width: 150 },
    { field: 'resourceId', headerName: 'Resource ID', width: 180 },
    { field: 'status', headerName: 'Status', width: 100 },
    { field: 'ipAddress', headerName: 'IP Address', width: 150 },
  ]

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Audit Logs
      </Typography>

      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <Box display="flex" gap={2} mb={2}>
          <DatePicker
            label="Start Date"
            value={startDate}
            onChange={(date) => setStartDate(date)}
            renderInput={(params) => <TextField {...params} />}
          />
          <DatePicker
            label="End Date"
            value={endDate}
            onChange={(date) => setEndDate(date)}
            renderInput={(params) => <TextField {...params} />}
          />
          <Button variant="contained" onClick={loadLogs}>
            Filter
          </Button>
          <Button
            variant="outlined"
            startIcon={<DownloadIcon />}
            onClick={handleExport}
          >
            Export CSV
          </Button>
        </Box>
      </LocalizationProvider>

      <Box sx={{ height: 600, width: '100%' }}>
        <DataGrid
          rows={logs}
          columns={columns}
          pageSize={50}
          rowsPerPageOptions={[50, 100]}
          onPageChange={(newPage) => setPage(newPage + 1)}
        />
      </Box>
    </Box>
  )
}
