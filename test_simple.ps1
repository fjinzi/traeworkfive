# Simple API Test Script for Seckill System - Focus on Edit Function
$baseUrl = "http://localhost:8080/api"

# Test 1: Register
Write-Host "=== Test 1: Register ===" -ForegroundColor Green
$random = Get-Random
$registerBody = @{
    username = "testuser_$random"
    password = "123456"
    nickname = "Test User"
    email = "test@test.com"
    phone = "13800138000"
} | ConvertTo-Json

Write-Host "Register Request: $registerBody" -ForegroundColor Gray
try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method POST -ContentType "application/json" -Body $registerBody
    Write-Host "Register Success: $($registerResponse.success)" -ForegroundColor Cyan
    Write-Host "Register Message: $($registerResponse.message)" -ForegroundColor Cyan
    if ($registerResponse.success -eq $true) {
        $token = $registerResponse.data.token
        $username = $registerResponse.data.username
        Write-Host "Token obtained successfully" -ForegroundColor Yellow
    } else {
        Write-Host "Register failed, trying login..." -ForegroundColor Yellow
        $loginBody = @{
            username = "testuser_$random"
            password = "123456"
        } | ConvertTo-Json
        $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
        Write-Host "Login Success: $($loginResponse.success)" -ForegroundColor Cyan
        if ($loginResponse.success -eq $true) {
            $token = $loginResponse.data.token
        } else {
            exit
        }
    }
} catch {
    Write-Host "Register Error: $_" -ForegroundColor Red
    exit
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Test 2: Create Product
Write-Host "`n=== Test 2: Create Product ===" -ForegroundColor Green
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
    Write-Host "Create Success: $($createResponse.success)" -ForegroundColor Cyan
    Write-Host "Create Message: $($createResponse.message)" -ForegroundColor Cyan
    if ($createResponse.success -eq $true) {
        $productId = $createResponse.data.id
        Write-Host "Created Product ID: $productId" -ForegroundColor Yellow
    } else {
        Write-Host "Create Product Failed: $($createResponse.message)" -ForegroundColor Red
        exit
    }
} catch {
    Write-Host "Create Product Error: $_" -ForegroundColor Red
    exit
}

# Test 3: Update Product (This is the key test for the fix)
Write-Host "`n=== Test 3: Update Product (KEY TEST) ===" -ForegroundColor Green
$updateStartTime = (Get-Date).AddHours(2).ToString("yyyy-MM-dd HH:mm:ss")
$updateEndTime = (Get-Date).AddHours(26).ToString("yyyy-MM-dd HH:mm:ss")

$updateBody = @{
    id = $productId
    name = "Updated Test Product $(Get-Date -Format 'yyyyMMddHHmmss')"
    stock = 200
    price = 88.88
    originalPrice = 188.88
    description = "Updated test product"
    imageUrl = "http://example.com/updated.jpg"
    startTime = $updateStartTime
    endTime = $updateEndTime
    status = 0
} | ConvertTo-Json

Write-Host "Update Request: $updateBody" -ForegroundColor Gray
try {
    $updateResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product" -Method PUT -Headers $headers -Body $updateBody
    Write-Host "Update Success: $($updateResponse.success)" -ForegroundColor Cyan
    Write-Host "Update Message: $($updateResponse.message)" -ForegroundColor Cyan
    if ($updateResponse.success -eq $true) {
        Write-Host "`n========================================" -ForegroundColor Green
        Write-Host "  UPDATE PRODUCT TEST PASSED!" -ForegroundColor Green
        Write-Host "========================================`n" -ForegroundColor Green
        Write-Host "Updated Product Name: $($updateResponse.data.name)" -ForegroundColor Yellow
        Write-Host "Updated Product Stock: $($updateResponse.data.stock)" -ForegroundColor Yellow
        Write-Host "Updated Product Price: $($updateResponse.data.price)" -ForegroundColor Yellow
    } else {
        Write-Host "`n========================================" -ForegroundColor Red
        Write-Host "  UPDATE PRODUCT TEST FAILED!" -ForegroundColor Red
        Write-Host "  Error: $($updateResponse.message)" -ForegroundColor Red
        Write-Host "========================================`n" -ForegroundColor Red
    }
} catch {
    Write-Host "`n========================================" -ForegroundColor Red
    Write-Host "  UPDATE PRODUCT TEST FAILED!" -ForegroundColor Red
    Write-Host "  Error: $_" -ForegroundColor Red
    Write-Host "========================================`n" -ForegroundColor Red
}

# Test 4: Get Product Page to verify
Write-Host "`n=== Test 4: Verify Update by Get Product Page ===" -ForegroundColor Green
try {
    $pageResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/page?pageNum=1&pageSize=10" -Method GET -Headers $headers
    Write-Host "Page Query Success: $($pageResponse.success)" -ForegroundColor Cyan
    if ($pageResponse.success -eq $true) {
        $updatedProduct = $pageResponse.data.records | Where-Object { $_.id -eq $productId }
        if ($updatedProduct) {
            Write-Host "Found updated product in list:" -ForegroundColor Yellow
            Write-Host "  - Name: $($updatedProduct.name)" -ForegroundColor Yellow
            Write-Host "  - Stock: $($updatedProduct.stock)" -ForegroundColor Yellow
            Write-Host "  - Price: $($updatedProduct.price)" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "Get Product Page Error: $_" -ForegroundColor Red
}

# Test 5: Delete Product
Write-Host "`n=== Test 5: Delete Product ===" -ForegroundColor Green
try {
    $deleteResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/$productId" -Method DELETE -Headers $headers
    Write-Host "Delete Success: $($deleteResponse.success)" -ForegroundColor Cyan
    Write-Host "Delete Message: $($deleteResponse.message)" -ForegroundColor Cyan
    if ($deleteResponse.success -eq $true) {
        Write-Host "DELETE PRODUCT TEST PASSED!" -ForegroundColor Green
    } else {
        Write-Host "DELETE PRODUCT TEST FAILED!" -ForegroundColor Red
    }
} catch {
    Write-Host "Delete Product Error: $_" -ForegroundColor Red
}

Write-Host "`n=== All Tests Completed ===" -ForegroundColor Green
