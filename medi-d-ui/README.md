# Medi-D UI - Frontend

Modern medical management system frontend built with React, Vite, and TypeScript.

## Features

- **Role-Based Dashboards**: Separate interfaces for Doctors, Receptionists, and Pharmacists
- **JWT Authentication**: Secure login with JWT tokens
- **Appointments Management**: Schedule and manage appointments
- **Prescriptions**: Manage patient prescriptions
- **Real-time API Integration**: Seamless integration with backend API
- **Responsive Design**: Mobile-friendly interface with Tailwind CSS
- **Type-Safe**: Full TypeScript support

## Tech Stack

- **React 18** - UI library
- **Vite** - Build tool
- **TypeScript** - Type safety
- **Tailwind CSS** - Styling
- **Radix UI** - Low-level UI components
- **Zustand** - State management
- **Axios** - HTTP client
- **React Router** - Routing

## Getting Started

### Prerequisites

- Node.js 16+ 
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Create environment file
cp .env.example .env
```

### Development

```bash
# Start development server
npm run dev
```

The app will be available at `http://localhost:3000`

### Build

```bash
# Build for production
npm run build
```

Output will be in the `dist` directory.

## Usage

### Login

Use one of the demo accounts:
- **Doctor**: username: `doctor`, password: `doctorpass`
- **Receptionist**: username: `receptionist`, password: `receptpass`
- **Pharmacist**: username: `pharmacist`, password: `pharmacistpass`

### API Configuration

The frontend proxies API requests to the backend. Update `VITE_API_URL` in `.env` if needed:

```env
VITE_API_URL=http://localhost:8080
```

## Project Structure

```
src/
├── app/              # Application root and route definitions
│   ├── App.tsx      # Main app component with routing
│   └── pages/       # Page components
├── components/       # Reusable components
│   └── ui/         # UI component library
├── lib/            # Utilities and stores
│   ├── api-client.ts    # API integration
│   ├── auth-store.ts    # Authentication state
│   └── utils.ts         # Utilities
└── styles/         # Global styles
```

## API Integration

All API calls are handled through `@/lib/api-client.ts`. The client automatically:
- Attaches JWT tokens from localStorage
- Refreshes tokens on 401 responses
- Handles error responses

Example usage:

```typescript
import { apiClient } from '@/lib/api-client'

const appointments = await apiClient.getAppointmentsByStatus('scheduled')
```

## Authentication

Authentication is managed through Zustand store in `@/lib/auth-store.ts`:

```typescript
import { useAuthStore } from '@/lib/auth-store'

export function MyComponent() {
  const { user, login, logout } = useAuthStore()
  
  return <div>{user?.username}</div>
}
```

## License

MIT
