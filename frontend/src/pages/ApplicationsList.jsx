// pages/ApplicationsList.jsx
import React, { useEffect, useState } from 'react';

export default function ApplicationsList() {
    const [applications, setApplications] = useState([]);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const apiUrl = import.meta.env.DEV
        ? 'http://localhost:8080/api'   // Если мы в режиме разработки
        : '/api';  // Если мы в продакшене

    useEffect(() => {
        fetchData(page);
    }, [page]);

    const fetchData = async (pageNum) => {
        try {
            const response = await fetch(`${apiUrl}/v1/credit-application/list?page=${pageNum}`);
            if (!response.ok) {
                throw new Error('Ошибка при загрузке заявок');
            }
            const data = await response.json();
            setApplications(data.content);
            setTotal(data.total);
            setPageSize(data.pageSize);
        } catch (error) {
            console.error(error);
        }
    };

    const handlePrev = () => {
        if (page > 1) setPage((prev) => prev - 1);
    };

    const handleNext = () => {
        const maxPage = Math.ceil(total / pageSize);
        if (page < maxPage) setPage((prev) => prev + 1);
    };

    return (
        <div className="card">
            <div className="card-header">
                <h3>Список заявок на кредит</h3>
            </div>
            <div className="card-body">
                <table className="table table-bordered">
                    <thead>
                    <tr>
                        <th>ID заявки</th>
                        <th>ID клиента</th>
                        <th>Запрошенная сумма</th>
                        <th>Одобренная сумма</th>
                        <th>Статус</th>
                    </tr>
                    </thead>
                    <tbody>
                    {applications.map((app) => (
                        <tr key={app.id}>
                            <td>{app.id}</td>
                            <td>{app.applicantId}</td>
                            <td>{app.requestedAmount}</td>
                            <td>{app.approvedAmount ?? '-'}</td>
                            <td>{app.status}</td>
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