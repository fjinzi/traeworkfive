# Full API Test Script for Seckill System
$baseUrl = "http://localhost:8080/api"

# Test 1: Register
Write-Host "=== Test 1: Register ===" -ForegroundColor Green
$registerBody = @{
    username = "testuser_$(Get-Random)"
    password = "123456"
    nickname = "Test User"
    email = "test@test.com"
    phone = "13800138000"
} | ConvertTo-Json

Write-Host "Register Request: $registerBody" -ForegroundColor Gray
try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method POST -ContentType "application/json" -Body $registerBody
    Write-Host "Register Response: $($registerResponse | ConvertTo-Json)" -ForegroundColor Cyan
    if ($registerResponse.success -eq $true) {
        $token = $registerResponse.data.token
        $username = $registerResponse.data.username
        Write-Host "Registered User: $username, Token: $token" -ForegroundColor Yellow
    } else {
        Write-Host "Register failed: $($registerResponse.message)" -ForegroundColor Red
        # Try login with existing user
        $username = ($registerBody | ConvertFrom-Json).username
        $loginBody = @{
            username = $username
            password = "123456"
        } | ConvertTo-Json
        $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
        if ($loginResponse.success -eq $true) {
            $token = $loginResponse.data.token
            Write-Host "Login successful, Token: $token" -ForegroundColor Yellow
        } else {
            Write-Host "Login also failed: $($loginResponse.message)" -ForegroundColor Red
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

# Test 2: Get User Info
Write-Host "`n=== Test 2: Get User Info ===" -ForegroundColor Green
try {
    $userInfo = Invoke-RestMethod -Uri "$baseUrl/auth/info" -Method GET -Headers $headers
    Write-Host "User Info: $($userInfo | ConvertTo-Json)" -ForegroundColor Cyan
    if ($userInfo.success -eq $false) {
        Write-Host "Get User Info Failed: $($userInfo.message)" -ForegroundColor Red
    }
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
    if ($createResponse.success -eq $true) {
        $productId = $createResponse.data.id
        Write-Host "Created Product ID: $productId" -ForegroundColor Yellow
    } else {
        Write-Host "Create Product Failed: $($createResponse.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "Create Product Error: $_" -ForegroundColor Red
}

# Test 4: Get Product Page
Write-Host "`n=== Test 4: Get Product Page ===" -ForegroundColor Green
try {
    $pageResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/page?pageNum=1&pageSize=10" -Method GET -Headers $headers
    Write-Host "Page Response: $($pageResponse | ConvertTo-Json)" -ForegroundColor Cyan
    if ($pageResponse.success -eq $false) {
        Write-Host "Get Product Page Failed: $($pageResponse.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "Get Product Page Error: $_" -ForegroundColor Red
}

# Test 5: Get Product by ID
if ($productId) {
    Write-Host "`n=== Test 5: Get Product by ID ===" -ForegroundColor Green
    try {
        $productResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/$productId" -Method GET -Headers $headers
        Write-Host "Product Response: $($productResponse | ConvertTo-Json)" -ForegroundColor Cyan
        if ($productResponse.success -eq $false) {
            Write-Host "Get Product Failed: $($productResponse.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "Get Product Error: $_" -ForegroundColor Red
    }
}

# Test 6: Update Product (This is the key test for the fix)
if ($productId) {
    Write-Host "`n=== Test 6: Update Product ===" -ForegroundColor Green
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
        Write-Host "Update Response: $($updateResponse | ConvertTo-Json)" -ForegroundColor Cyan
        if ($updateResponse.success -eq $true) {
            Write-Host "UPDATE PRODUCT TEST PASSED!" -ForegroundColor Green
        } else {
            Write-Host "UPDATE PRODUCT TEST FAILED: $($updateResponse.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "Update Product Error: $_" -ForegroundColor Red
    }
}

# Test 7: Sync Product to Redis
if ($productId) {
    Write-Host "`n=== Test 7: Sync Product to Redis ===" -ForegroundColor Green
    try {
        $syncResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/sync/$productId" -Method POST -Headers $headers
        Write-Host "Sync Response: $($syncResponse | ConvertTo-Json)" -ForegroundColor Cyan
        if ($syncResponse.success -eq $true) {
            Write-Host "SYNC PRODUCT TEST PASSED!" -ForegroundColor Green
        } else {
            Write-Host "SYNC PRODUCT TEST FAILED: $($syncResponse.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "Sync Product Error: $_" -ForegroundColor Red
    }
}

# Test 8: Sync All to Redis
Write-Host "`n=== Test 8: Sync All to Redis ===" -ForegroundColor Green
try {
    $syncAllResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/sync-all" -Method POST -Headers $headers
    Write-Host "Sync All Response: $($syncAllResponse | ConvertTo-Json)" -ForegroundColor Cyan
    if ($syncAllResponse.success -eq $true) {
        Write-Host "SYNC ALL TEST PASSED!" -ForegroundColor Green
    } else {
        Write-Host "SYNC ALL TEST FAILED: $($syncAllResponse.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "Sync All Error: $_" -ForegroundColor Red
}

# Test 9: Update Status
Write-Host "`n=== Test 9: Update Status ===" -ForegroundColor Green
try {
    $updateStatusResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/update-status" -Method POST -Headers $headers
    Write-Host "Update Status Response: $($updateStatusResponse | ConvertTo-Json)" -ForegroundColor Cyan
    if ($updateStatusResponse.success -eq $true) {
        Write-Host "UPDATE STATUS TEST PASSED!" -ForegroundColor Green
    } else {
        Write-Host "UPDATE STATUS TEST FAILED: $($updateStatusResponse.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "Update Status Error: $_" -ForegroundColor Red
}

# Test 10: Get Active Products
Write-Host "`n=== Test 10: Get Active Products ===" -ForegroundColor Green
try {
    $activeResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/active" -Method GET -Headers $headers
    Write-Host "Active Products Response: $($activeResponse | ConvertTo-Json)" -ForegroundColor Cyan
    if ($activeResponse.success -eq $true) {
        Write-Host "GET ACTIVE PRODUCTS TEST PASSED!" -ForegroundColor Green
    } else {
        Write-Host "GET ACTIVE PRODUCTS TEST FAILED: $($activeResponse.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "Get Active Products Error: $_" -ForegroundColor Red
}

# Test 11: Delete Product
if ($productId) {
    Write-Host "`n=== Test 11: Delete Product ===" -ForegroundColor Green
    try {
        $deleteResponse = Invoke-RestMethod -Uri "$baseUrl/admin/seckill/product/$productId" -Method DELETE -Headers $headers
        Write-Host "Delete Response: $($deleteResponse | ConvertTo-Json)" -ForegroundColor Cyan
        if ($deleteResponse.success -eq $true) {
            Write-Host "DELETE PRODUCT TEST PASSED!" -ForegroundColor Green
        } else {
            Write-Host "DELETE PRODUCT TEST FAILED: $($deleteResponse.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "Delete Product Error: $_" -ForegroundColor Red
    }
}

# Test 12: Get Public Products (No auth required)
Write-Host "`n=== Test 12: Get Public Products ===" -ForegroundColor Green
try {
    $publicResponse = Invoke-RestMethod -Uri "$baseUrl/seckill/products" -Method GET
    Write-Host "Public Products Response: $($publicResponse | ConvertTo-Json)" -ForegroundColor Cyan
    if ($publicResponse.success -eq $true) {
        Write-Host "GET PUBLIC PRODUCTS TEST PASSED!" -ForegroundColor Green
    } else {
        Write-Host "GET PUBLIC PRODUCTS TEST FAILED: $($publicResponse.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "Get Public Products Error: $_" -ForegroundColor Red
}

Write-Host "`n=== All Tests Completed ===" -ForegroundColor Green
