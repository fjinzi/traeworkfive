# API Test Script for Seckill System
$baseUrl = "http://localhost:8080/api"

# Test 1: Login
Write-Host "=== Test 1: Login ===" -ForegroundColor Green
$loginBody = @{
    username = "admin"
    password = "123456"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
    Write-Host "Login Response: $($loginResponse | ConvertTo-Json)" -ForegroundColor Cyan
    $token = $loginResponse.data.token
    Write-Host "Token: $token" -ForegroundColor Yellow
} catch {
    Write-Host "Login Error: $_" -ForegroundColor Red
    exit
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Test 2: Get User Info
Write-Host "`n=== Test 2: Get User Info ===" -ForegroundColor Green
try {
    $userInfo = Invoke-RestMethod -Uri "$baseUrl/auth/info" -Method GET -Headers $headers
    Write-Host "User Info: $($userInfo | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "Get User Info Error: $_" -ForegroundColor Red
}

# Test 3: Create Product
Write-Host "`n=== Test 3: Create Product ===" -ForegroundColor Green
$startTime = (Get-Date).AddHours(1).ToString("yyyy-MM-dd HH:mm:ss")
$endTime = (Get-Date).AddHours(25).ToString("yyyy-MM-dd HH:mm:ss")

$createBody = @{
    name = "Test Product $(Get-Date -Format 'yyyyMMddHHmmss')"
    stock = 100
    price = 99.99
    originalPrice = 199.99
    description = "Test product created by API test"
    imageUrl = "http://example.com/image.jpg"
    startTime = $startTime
    endTime = $endTime
} | ConvertTo-Json

Write-Host "Create Request: $createBody" -ForegroundColor Gray
try {
    $createResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product" -Method POST -Headers $headers -Body $createBody
    Write-Host "Create Response: $($createResponse | ConvertTo-Json)" -ForegroundColor Cyan
    $productId = $createResponse.data.id
    Write-Host "Created Product ID: $productId" -ForegroundColor Yellow
} catch {
    Write-Host "Create Product Error: $_" -ForegroundColor Red
}

# Test 4: Get Product Page
Write-Host "`n=== Test 4: Get Product Page ===" -ForegroundColor Green
try {
    $pageResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/page?pageNum=1&pageSize=10" -Method GET -Headers $headers
    Write-Host "Page Response: $($pageResponse | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "Get Product Page Error: $_" -ForegroundColor Red
}

# Test 5: Get Product by ID
Write-Host "`n=== Test 5: Get Product by ID ===" -ForegroundColor Green
try {
    $productResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/$productId" -Method GET -Headers $headers
    Write-Host "Product Response: $($productResponse | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "Get Product Error: $_" -ForegroundColor Red
}

# Test 6: Update Product
Write-Host "`n=== Test 6: Update Product ===" -ForegroundColor Green
$updateBody = @{
    id = $productId
    name = "Updated Test Product $(Get-Date -Format 'yyyyMMddHHmmss')"
    stock = 200
    price = 88.88
    originalPrice = 188.88
    description = "Updated test product"
    imageUrl = "http://example.com/updated.jpg"
    startTime = $startTime
    endTime = $endTime
    status = 0
} | ConvertTo-Json

Write-Host "Update Request: $updateBody" -ForegroundColor Gray
try {
    $updateResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product" -Method PUT -Headers $headers -Body $updateBody
    Write-Host "Update Response: $($updateResponse | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "Update Product Error: $_" -ForegroundColor Red
}

# Test 7: Sync Product to Redis
Write-Host "`n=== Test 7: Sync Product to Redis ===" -ForegroundColor Green
try {
    $syncResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/sync/$productId" -Method POST -Headers $headers
    Write-Host "Sync Response: $($syncResponse | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "Sync Product Error: $_" -ForegroundColor Red
}

# Test 8: Sync All to Redis
Write-Host "`n=== Test 8: Sync All to Redis ===" -ForegroundColor Green
try {
    $syncAllResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/sync-all" -Method POST -Headers $headers
    Write-Host "Sync All Response: $($syncAllResponse | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "Sync All Error: $_" -ForegroundColor Red
}

# Test 9: Update Status
Write-Host "`n=== Test 9: Update Status ===" -ForegroundColor Green
try {
    $updateStatusResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/update-status" -Method POST -Headers $headers
    Write-Host "Update Status Response: $($updateStatusResponse | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "Update Status Error: $_" -ForegroundColor Red
}

# Test 10: Delete Product
Write-Host "`n=== Test 10: Delete Product ===" -ForegroundColor Green
try {
    $deleteResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/$productId" -Method DELETE -Headers $headers
    Write-Host "Delete Response: $($deleteResponse | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "Delete Product Error: $_" -ForegroundColor Red
}

# Test 11: Get Active Products (Public API)
Write-Host "`n=== Test 11: Get Active Products ===" -ForegroundColor Green
try {
    $activeResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/active" -Method GET -Headers $headers
    Write-Host "Active Products Response: $($activeResponse | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "Get Active Products Error: $_" -ForegroundColor Red
}

Write-Host "`n=== All Tests Completed ===" -ForegroundColor Green
