import React from 'react';
import AuthLayout from '../layouts/AuthLayout.jsx';
import AuthForm from '../components/auth/AuthForm.jsx';

export default function LoginPage({ navigate, onLogin }) {
  return (
    <AuthLayout label="Log in to your account" navigate={navigate}>
      <AuthForm signup={false} navigate={navigate} onLogin={onLogin} />
    </AuthLayout>
  );
}
