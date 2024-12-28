import React from 'react';
import { NavLink } from 'react-router-dom';

export default function Navbar() {
    return (
        <nav className="navbar navbar-expand-lg navbar-dark bg-dark">
            <div className="container-fluid">
                <NavLink to="/" className="navbar-brand">
                    Быстроденьги от Димана
                </NavLink>
                <button
                    className="navbar-toggler"
                    type="button"
                    data-bs-toggle="collapse"
                    data-bs-target="#navbarNav"
                    aria-controls="navbarNav"
                    aria-expanded="false"
                    aria-label="Переключить навигацию"
                >
                    <span className="navbar-toggler-icon"></span>
                </button>

                <div className="collapse navbar-collapse" id="navbarNav">
                    <ul className="navbar-nav ms-auto">
                        <li className="nav-item">
                            <NavLink
                                to="/application-form"
                                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                            >
                                Новая заявка
                            </NavLink>
                        </li>
                        <li className="nav-item">
                            <NavLink
                                to="/applications"
                                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                            >
                                Заявки
                            </NavLink>
                        </li>
                        <li className="nav-item">
                            <NavLink
                                to="/agreements"
                                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                            >
                                Договоры
                            </NavLink>
                        </li>
                        <li className="nav-item">
                            <NavLink
                                to="/clients"
                                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                            >
                                Клиенты
                            </NavLink>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
    );
}
