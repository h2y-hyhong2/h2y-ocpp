param(
    [Parameter(Mandatory = $true)][string]$SourcePath,
    [Parameter(Mandatory = $true)][string]$OutPngPath,
    [string]$OutIcoPath = "",
    [string]$OutRgba64Path = "",
    [int]$Size = 1024,
    [int]$CornerRadius = 224
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function New-RoundedSquareBitmap {
    param(
        [string]$SrcFile,
        [int]$TargetSize,
        [int]$Radius
    )
    $srcImg = [System.Drawing.Image]::FromFile($SrcFile)
    try {
        $bmp = New-Object System.Drawing.Bitmap($TargetSize, $TargetSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        try {
            $g.Clear([System.Drawing.Color]::Transparent)
            $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

            if ($Radius -gt 0) {
                $path = New-Object System.Drawing.Drawing2D.GraphicsPath
                $d = $Radius * 2
                $path.AddArc(0, 0, $d, $d, 180, 90)
                $path.AddArc($TargetSize - $d, 0, $d, $d, 270, 90)
                $path.AddArc($TargetSize - $d, $TargetSize - $d, $d, $d, 0, 90)
                $path.AddArc(0, $TargetSize - $d, $d, $d, 90, 90)
                $path.CloseFigure()
                $g.SetClip($path)
            }

            # Center-crop to square if aspect differs
            $side = [Math]::Min($srcImg.Width, $srcImg.Height)
            $sx = [int](($srcImg.Width - $side) / 2)
            $sy = [int](($srcImg.Height - $side) / 2)
            $srcRect = New-Object System.Drawing.Rectangle($sx, $sy, $side, $side)
            $dstRect = New-Object System.Drawing.Rectangle(0, 0, $TargetSize, $TargetSize)
            $g.DrawImage($srcImg, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
            if ($Radius -gt 0) {
                $path.Dispose()
            }
        } finally {
            $g.Dispose()
        }
        return $bmp
    } finally {
        $srcImg.Dispose()
    }
}

function Resize-Bitmap {
    param(
        [System.Drawing.Bitmap]$Source,
        [int]$W,
        [int]$H
    )
    $dst = New-Object System.Drawing.Bitmap($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($dst)
    try {
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $g.DrawImage($Source, 0, 0, $W, $H)
    } finally {
        $g.Dispose()
    }
    return $dst
}

function Write-MultiSizeIco {
    param(
        [System.Drawing.Bitmap]$MasterBmp,
        [string]$IcoPath
    )
    $sizes = @(16, 24, 32, 48, 64, 128, 256)
    $pngBlobs = @()
    foreach ($sz in $sizes) {
        $resized = Resize-Bitmap -Source $MasterBmp -W $sz -H $sz
        try {
            $ms = New-Object System.IO.MemoryStream
            $resized.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
            $pngBlobs += [PSCustomObject]@{ Size = $sz; Bytes = $ms.ToArray() }
            $ms.Dispose()
        } finally {
            $resized.Dispose()
        }
    }

    $dir = [System.IO.Path]::GetDirectoryName($IcoPath)
    if ($dir -and -not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }

    $fs = [System.IO.File]::Open($IcoPath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
    $bw = New-Object System.IO.BinaryWriter($fs)
    try {
        # ICONDIR header
        $bw.Write([UInt16]0)
        $bw.Write([UInt16]1)
        $bw.Write([UInt16]$pngBlobs.Count)

        $offset = 6 + (16 * $pngBlobs.Count)
        foreach ($entry in $pngBlobs) {
            $dimByte = if ($entry.Size -ge 256) { [byte]0 } else { [byte]$entry.Size }
            $bw.Write($dimByte)              # Width
            $bw.Write($dimByte)              # Height
            $bw.Write([byte]0)               # ColorCount
            $bw.Write([byte]0)               # Reserved
            $bw.Write([UInt16]1)             # Planes
            $bw.Write([UInt16]32)            # BitCount
            $bw.Write([UInt32]$entry.Bytes.Length)
            $bw.Write([UInt32]$offset)
            $offset += $entry.Bytes.Length
        }

        foreach ($entry in $pngBlobs) {
            $bw.Write($entry.Bytes)
        }
    } finally {
        $bw.Dispose()
        $fs.Dispose()
    }
}

function Write-Rgba64 {
    param(
        [System.Drawing.Bitmap]$MasterBmp,
        [string]$RgbaPath
    )
    $resized = Resize-Bitmap -Source $MasterBmp -W 64 -H 64
    try {
        $rgba = New-Object byte[] (64 * 64 * 4)
        $idx = 0
        for ($y = 0; $y -lt 64; $y++) {
            for ($x = 0; $x -lt 64; $x++) {
                $c = $resized.GetPixel($x, $y)
                $rgba[$idx++] = $c.R
                $rgba[$idx++] = $c.G
                $rgba[$idx++] = $c.B
                $rgba[$idx++] = $c.A
            }
        }
        $dir = [System.IO.Path]::GetDirectoryName($RgbaPath)
        if ($dir -and -not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
        [System.IO.File]::WriteAllBytes($RgbaPath, $rgba)
    } finally {
        $resized.Dispose()
    }
}

$outDir = [System.IO.Path]::GetDirectoryName($OutPngPath)
if ($outDir -and -not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

$master = New-RoundedSquareBitmap -SrcFile $SourcePath -TargetSize $Size -Radius $CornerRadius
try {
    $master.Save($OutPngPath, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Host "[icon] PNG 저장: $OutPngPath ($($Size)x$($Size))"

    if ($OutIcoPath) {
        Write-MultiSizeIco -MasterBmp $master -IcoPath $OutIcoPath
        Write-Host "[icon] ICO 저장: $OutIcoPath (16/24/32/48/64/128/256)"
    }
    if ($OutRgba64Path) {
        Write-Rgba64 -MasterBmp $master -RgbaPath $OutRgba64Path
        Write-Host "[icon] RGBA64 저장: $OutRgba64Path (64x64)"
    }
} finally {
    $master.Dispose()
}
