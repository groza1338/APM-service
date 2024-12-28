import React from 'react';
import { Link } from 'react-router-dom';

export default function WelcomePage() {
    return (
        <div className="text-center">
            <h1 className="display-4 fw-bold">Добро пожаловать в наше кредитное приложение!</h1>
            <p className="lead mt-3">
                Мы предоставляем быстрые и удобные решения для получения кредита,
                помогая вам достигать ваших целей. Создавайте заявки, управляйте договорами
                и отслеживайте клиентов в одном месте. С нами ваши финансовые возможности
                безграничны.
            </p>
            <div className="mt-4">
                <Link to="/application-form" className="btn btn-primary btn-lg">
                    Создать заявку на кредит
                </Link>
            </div>
        </div>
    );
}
