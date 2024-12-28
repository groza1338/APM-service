// main.jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import App from './App';
import ApplicationForm from './pages/ApplicationForm';
import ApplicationsList from './pages/ApplicationsList';
import AgreementsList from './pages/AgreementsList';
import ClientsList from './pages/ClientsList';
import WelcomePage from "./pages/WelcomePage.jsx";

const router = createBrowserRouter([
    {
        path: '/',
        element: <App />,
        children: [
            { index: true, element: <WelcomePage /> },
            { path: 'application-form', element: <ApplicationForm /> },
            { path: 'applications', element: <ApplicationsList /> },
            { path: 'agreements', element: <AgreementsList /> },
            { path: 'clients', element: <ClientsList /> },
        ],
    },
]);

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <RouterProvider router={router} />
    </React.StrictMode>
);