import React from 'react';
import AuthLayout from '../layouts/AuthLayout.jsx';
import AuthForm from '../components/auth/AuthForm.jsx';

export default function SignupPage({ navigate }) {
  return (
    <AuthLayout label="Create an account" navigate={navigate}>
      <AuthForm signup navigate={navigate} />
    </AuthLayout>
  );
}
