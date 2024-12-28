// pages/ApplicationForm.jsx
import React, { useState } from 'react';

export default function ApplicationForm() {
    const [formData, setFormData] = useState({
        firstName: '',
        lastName: '',
        middleName: '',
        passport: '',
        maritalStatus: 'NEVER_MARRIED',
        address: '',
        phone: '',
        organizationName: '',
        position: '',
        employmentPeriod: '',
        amount: ''
    });
    const apiUrl = import.meta.env.DEV
        ? 'http://localhost:8080/api'   // Если мы в режиме разработки
        : '/api';  // Если мы в продакшене
    const [applicationResult, setApplicationResult] = useState(null);
    const [errorMessage, setErrorMessage] = useState('');
    const [isSigned, setIsSigned] = useState(false); // Следим, подписан ли договор

    const handleChange = (e) => {
        const { name, value } = e.target;

        // Шаблоны для разных полей
        const patterns = {
            firstName: /^(?:[A-Z][a-z]*(?:[ '-][A-Za-z]+)*)?$/, // Имя: буквы, пробел, дефис, апостроф
            lastName: /^(?:[A-Z][a-z]*(?:[ '-][A-Za-z]+)*)?$/,  // Фамилия: аналогично имени
            middleName: /^(?:[A-Z][a-z]*(?:[ '-][A-Za-z]+)*)?$/, // Отчество: аналогично имени
            passport: /^\d*$/,           // Паспорт: только цифры
            phone: /^\+?\d*$/,           // Телефон: только цифры, можно +
            employmentPeriod: /^$|^PT?$|^PT\d*$|^PT\d+H$/, // Стаж: формат PT...H
            amount: /^\d*\.?\d{0,2}$/,   // Сумма: целые или десятичные числа
        };
        // Проверяем шаблон, если он есть для текущего поля
        if (patterns[name] && !patterns[name].test(value)) {
            return; // Если значение не соответствует шаблону, не обновляем state
        }

        setFormData((prev) => ({ ...prev, [name]: value }));
    };


    const handleSubmit = async (e) => {
        e.preventDefault();
        setApplicationResult(null);
        setErrorMessage('');
        setIsSigned(false);

        try {
            const response = await fetch(`${apiUrl}/v1/credit-application`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    applicant: {
                        firstName: formData.firstName,
                        lastName: formData.lastName,
                        middleName: formData.middleName || undefined, // Можно передавать undefined, если пустое
                        passport: formData.passport,
                        maritalStatus: formData.maritalStatus,
                        address: formData.address,
                        phone: formData.phone,
                        organizationName: formData.organizationName,
                        position: formData.position,
                        employmentPeriod: formData.employmentPeriod
                    },
                    amount: parseFloat(formData.amount)
                }),
            });

            if (!response.ok) {
                throw new Error('Ошибка при создании заявки');
            }

            const data = await response.json();
            setApplicationResult(data);
        } catch (error) {
            setErrorMessage(error.message);
        }
    };

    const handleSign = async () => {
        if (!applicationResult || !applicationResult.id) return;

        try {
            const response = await fetch(
                `${apiUrl}/v1/credit-application/${applicationResult.id}/signing`,
                {
                    method: 'PATCH'
                }
            );
            if (!response.ok) {
                throw new Error('Ошибка при подписании договора');
            }
            // Если всё ок, устанавливаем флаг, что договор подписан
            setIsSigned(true);
        } catch (error) {
            setErrorMessage(error.message);
        }
    };

    return (
        <div className="card">
            <div className="card-header">
                <h3>Новая заявка на кредит</h3>
            </div>
            <div className="card-body">
                <form onSubmit={handleSubmit}>
                    {/* Имя */}
                    <div className="mb-3">
                        <label className="form-label">Имя (1-64 символов, с заглавной буквы)</label>
                        <input
                            name="firstName"
                            required
                            minLength={1}
                            maxLength={64}
                            className="form-control"
                            value={formData.firstName}
                            onChange={handleChange}
                        />
                    </div>


                    {/* Фамилия */}
                    <div className="mb-3">
                        <label className="form-label">Фамилия (1-64 символов, с заглавной буквы)</label>
                        <input
                            name="lastName"
                            required
                            minLength={1}
                            maxLength={64}
                            className="form-control"
                            value={formData.lastName}
                            onChange={handleChange}
                        />
                    </div>

                    {/* Отчество (необязательно, но если введено, должно соответствовать формату) */}
                    <div className="mb-3">
                        <label className="form-label">Отчество (макс. 64, с заглавной буквы)</label>
                        <input
                            name="middleName"
                            minLength={1}
                            maxLength={64}
                            className="form-control"
                            value={formData.middleName}
                            onChange={handleChange}
                        />
                    </div>

                    {/* Паспорт: ровно 10 цифр */}
                    <div className="mb-3">
                        <label className="form-label">Паспорт (10 цифр)</label>
                        <input
                            name="passport"
                            required
                            minLength={10}
                            maxLength={10}
                            className="form-control"
                            value={formData.passport}
                            onChange={handleChange}
                        />
                    </div>

                    {/* Семейное положение */}
                    <div className="mb-3">
                        <label className="form-label">Семейное положение</label>
                        <select
                            name="maritalStatus"
                            required
                            className="form-select"
                            value={formData.maritalStatus}
                            onChange={handleChange}
                        >
                            <option value="NEVER_MARRIED">NEVER_MARRIED</option>
                            <option value="MARRIED">MARRIED</option>
                            <option value="DIVORCED">DIVORCED</option>
                            <option value="WIDOWED">WIDOWED</option>
                            <option value="COHABITING">COHABITING</option>
                            <option value="SEPARATED">SEPARATED</option>
                        </select>
                    </div>

                    {/* Адрес (1-128 символов) */}
                    <div className="mb-3">
                        <label className="form-label">Адрес (1-128 символов)</label>
                        <input
                            name="address"
                            required
                            minLength={1}
                            maxLength={128}
                            className="form-control"
                            value={formData.address}
                            onChange={handleChange}
                        />
                    </div>

                    {/* Телефон: +12345678901 или 12345678901 (11 цифр) */}
                    <div className="mb-3">
                        <label className="form-label">
                            Телефон (11 цифр, можно + в начале, напр. +12345678901)
                        </label>
                        <input
                            name="phone"
                            required
                            minLength={11}
                            maxLength={12}
                            className="form-control"
                            value={formData.phone}
                            onChange={handleChange}
                        />
                    </div>

                    {/* Организация (1-96 символов) */}
                    <div className="mb-3">
                        <label className="form-label">Организация (1-96 символов)</label>
                        <input
                            name="organizationName"
                            required
                            minLength={1}
                            maxLength={96}
                            className="form-control"
                            value={formData.organizationName}
                            onChange={handleChange}
                        />
                    </div>

                    {/* Должность (1-64 символов) */}
                    <div className="mb-3">
                        <label className="form-label">Должность (1-64 символов)</label>
                        <input
                            name="position"
                            required
                            minLength={1}
                            maxLength={64}
                            className="form-control"
                            value={formData.position}
                            onChange={handleChange}
                        />
                    </div>

                    {/* Стаж (required), формируем строку вида PT24H, PT10H и т.д. */}
                    <div className="mb-3">
                        <label className="form-label">Стаж (формат: PT...H, напр. PT72H)</label>
                        <input
                            name="employmentPeriod"
                            required
                            maxLength={16}
                            className="form-control"
                            value={formData.employmentPeriod}
                            onChange={handleChange}
                        />
                    </div>

                    {/* Сумма (min=1, step=0.01) */}
                    <div className="mb-3">
                        <label className="form-label">Сумма (минимум 1, например 15000.50)</label>
                        <input
                            name="amount"
                            required
                            type="text"
                            className="form-control"
                            value={formData.amount}
                            onChange={handleChange}
                            maxLength={10}
                        />


                    </div>

                    <button type="submit" className="btn btn-primary">
                        Отправить заявку
                    </button>
                </form>

                {errorMessage && <p className="text-danger mt-3">{errorMessage}</p>}

                {applicationResult && (
                    <div className="alert alert-info mt-4">
                        <h5>Результат заявки</h5>
                        <p>
                            <strong>ID заявки:</strong> {applicationResult.id}
                        </p>
                        <p>
                            <strong>Статус:</strong> {applicationResult.status}
                        </p>
                        {applicationResult.status === 'APPROVED' && (
                            <p>
                                <strong>Одобренная сумма:</strong> {applicationResult.approvedAmount}
                                <br/>
                                <strong>Утверждённый срок:</strong> {applicationResult.approvedTerm}
                            </p>
                        )}

                        {/* Если заявка одобрена, показываем либо кнопку подписания, либо сообщение о подписании */}
                        {applicationResult.status === 'APPROVED' && !isSigned && (
                            <button className="btn btn-success" onClick={handleSign}>
                                Подписать договор
                            </button>
                        )}
                        {applicationResult.status === 'APPROVED' && isSigned && (
                            <p className="text-success">Договор успешно подписан!</p>
                        )}

                        {applicationResult.status === 'REJECTED' && (
                            <p className="text-danger">Ваша заявка отклонена.</p>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}