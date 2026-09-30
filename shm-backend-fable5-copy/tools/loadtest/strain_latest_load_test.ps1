# Load simulation for the snapshot backend rate limiter.
# Fires 20 parallel POST /api/data/strain/latest requests from one
# client IP and reports the status code distribution (200 vs 429 vs 5xx).

$jobs = 1..20 | ForEach-Object {
  Start-Job {
    try {
      $response = Invoke-WebRequest -Method Post `
        -Uri "http://localhost:8080/api/data/strain/latest" `
        -ContentType "application/json" `
        -Body '{"sensorId":"FBG-STRAIN-CH2"}' `
        -UseBasicParsing `
        -ErrorAction Stop

      [PSCustomObject]@{
        StatusCode = [int]$response.StatusCode
        Success = $true
        Body = $response.Content
      }
    } catch {
      $status = $null
      $body = $null

      if ($_.Exception.Response) {
        $status = [int]$_.Exception.Response.StatusCode
        try {
          $stream = $_.Exception.Response.GetResponseStream()
          if ($stream) {
            $reader = New-Object System.IO.StreamReader($stream)
            $body = $reader.ReadToEnd()
          }
        } catch {
          $body = $null
        }
      }

      [PSCustomObject]@{
        StatusCode = $status
        Success = $false
        Error = $_.Exception.Message
        Body = $body
      }
    }
  }
}

$results = $jobs | Wait-Job | Receive-Job

Write-Host "=== Status code distribution ==="
$results | Group-Object StatusCode | Select-Object Name, Count | Format-Table -AutoSize

$sample429 = $results | Where-Object { $_.StatusCode -eq 429 } | Select-Object -First 1
if ($sample429) {
  Write-Host "=== Sample 429 body ==="
  Write-Host $sample429.Body
}

$sample200 = $results | Where-Object { $_.StatusCode -eq 200 } | Select-Object -First 1
if ($sample200) {
  Write-Host "=== Sample 200 body (truncated) ==="
  $text = [string]$sample200.Body
  if ($text.Length -gt 300) { $text = $text.Substring(0, 300) }
  Write-Host $text
}

$jobs | Remove-Job
