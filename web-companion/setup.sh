#!/bin/bash

echo "🚀 ObsidianBackup Web Companion Setup"
echo "======================================"
echo ""

# Check Node.js version
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 18+ first."
    exit 1
fi

NODE_VERSION=$(node -v | cut -d 'v' -f 2 | cut -d '.' -f 1)
if [ "$NODE_VERSION" -lt 18 ]; then
    echo "❌ Node.js version 18+ required. Current version: $(node -v)"
    exit 1
fi

echo "✅ Node.js $(node -v) detected"
echo ""

# Install frontend dependencies
echo "📦 Installing frontend dependencies..."
npm install

if [ $? -ne 0 ]; then
    echo "❌ Failed to install frontend dependencies"
    exit 1
fi

echo "✅ Frontend dependencies installed"
echo ""

# Install backend dependencies
echo "📦 Installing backend dependencies..."
cd server
npm install

if [ $? -ne 0 ]; then
    echo "❌ Failed to install backend dependencies"
    exit 1
fi

cd ..
echo "✅ Backend dependencies installed"
echo ""

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo "📝 Creating .env file..."
    cp .env.example .env
    echo "✅ .env file created. Please edit it with your configuration."
else
    echo "ℹ️  .env file already exists"
fi

echo ""
echo "✨ Setup complete!"
echo ""
echo "Next steps:"
echo "1. Edit .env file with your configuration"
echo "2. Start the development servers:"
echo "   npm run dev:all"
echo ""
echo "3. Open http://localhost:3000 in your browser"
echo ""
echo "For production build:"
echo "   npm run build"
echo ""
