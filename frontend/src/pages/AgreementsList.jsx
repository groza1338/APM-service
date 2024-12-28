// pages/AgreementsList.jsx
import React, { useEffect, useState } from 'react';

export default function AgreementsList() {
    const [agreements, setAgreements] = useState([]);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const apiUrl = import.meta.env.DEV
        ? 'http://localhost:8080/api'   // Если мы в режиме разработки
        : '/api';  // Если мы в продакшене

    useEffect(() => {
        fetchAgreements(page);
    }, [page]);

    const fetchAgreements = async (pageNum) => {
        try {
            const response = await fetch(
                `${apiUrl}/v1/credit-application/list-agreement?page=${pageNum}`
            );
            if (!response.ok) {
                throw new Error('Ошибка при загрузке договоров');
            }
            const data = await response.json();
            setAgreements(data.content);
            setTotal(data.total);
            setPageSize(data.pageSize);
        } catch (error) {
            console.error(error);
        }
    };

    const handlePrev = () => {
        if (page > 1) setPage((p) => p - 1);
    };

    const handleNext = () => {
        const maxPage = Math.ceil(total / pageSize);
        if (page < maxPage) setPage((p) => p + 1);
    };

    return (
        <div className="card">
            <div className="card-header">
                <h3>Список кредитных договоров</h3>
            </div>
            <div className="card-body">
                <table className="table table-bordered">
                    <thead>
                    <tr>
                        <th>ID договора</th>
                        <th>ID заявки</th>
                        <th>Статус подписания</th>
                        <th>Дата подписания</th>
                    </tr>
                    </thead>
                    <tbody>
                    {agreements.map((agreement) => (
                        <tr key={agreement.id}>
                            <td>{agreement.id}</td>
                            <td>{agreement.applicationId}</td>
                            <td>{agreement.signingStatus}</td>
                            <td>{agreement.signedAt || '-'}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>

                <div className="d-flex justify-content-between align-items-center">
                    <button className="btn btn-secondary" onClick={handlePrev} disabled={page === 1}>
                        Назад
                    </button>
                    <span>Страница {page}</span>
                    <button className="btn btn-secondary" onClick={handleNext} disabled={page * pageSize >= total}>
                        Вперёд
                    </button>
                </div>
            </div>
        </div>
    );
}