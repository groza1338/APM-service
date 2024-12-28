// pages/ClientsList.jsx
import React, { useEffect, useState } from 'react';

export default function ClientsList() {
    const [clients, setClients] = useState([]);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const apiUrl = import.meta.env.DEV
        ? 'http://localhost:8080/api'   // Если мы в режиме разработки
        : '/api';  // Если мы в продакшене

    // Объект с полями для поиска
    const [searchData, setSearchData] = useState({
        firstName: '',
        lastName: '',
        middleName: '',
        phone: '',
        passport: ''
    });

    useEffect(() => {
        // При каждом изменении page вызываем загрузку клиентов
        // Передаём searchData, потому что пользователь мог уже что-то ввести
        fetchClients(page, searchData);
    }, [page]);

    // Запрос на сервер
    const fetchClients = async (pageNum, searchValues) => {
        try {
            // Начинаем формировать URL
            let url = `${apiUrl}/v1/client/list?page=${pageNum}`;

            // Сериализуем только непустые поля
            const queryParams = [];
            if (searchValues.firstName) {
                queryParams.push(`firstName=${encodeURIComponent(searchValues.firstName)}`);
            }
            if (searchValues.lastName) {
                queryParams.push(`lastName=${encodeURIComponent(searchValues.lastName)}`);
            }
            if (searchValues.middleName) {
                queryParams.push(`middleName=${encodeURIComponent(searchValues.middleName)}`);
            }
            if (searchValues.phone) {
                queryParams.push(`phone=${encodeURIComponent(searchValues.phone)}`);
            }
            if (searchValues.passport) {
                queryParams.push(`passport=${encodeURIComponent(searchValues.passport)}`);
            }

            // Если что-то есть, добавляем в url
            if (queryParams.length > 0) {
                url += '&' + queryParams.join('&');
            }

            const response = await fetch(url);

            if (!response.ok) {
                throw new Error('Ошибка при загрузке клиентов');
            }

            const data = await response.json();
            setClients(data.content);
            setTotal(data.total);
            setPageSize(data.pageSize);
        } catch (error) {
            console.error(error);
        }
    };

    // Функция для переключения на предыдущую страницу
    const handlePrev = () => {
        if (page > 1) {
            setPage((prev) => prev - 1);
        }
    };

    // Функция для переключения на следующую страницу
    const handleNext = () => {
        const maxPage = Math.ceil(total / pageSize);
        if (page < maxPage) {
            setPage((prev) => prev + 1);
        }
    };

    // При изменении любого поля поиска обновляем состояние
    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setSearchData((prev) => ({
            ...prev,
            [name]: value
        }));
    };

    // При сабмите формы сбрасываем страницу в 1 и вызываем fetch с текущими значениями полей
    const handleSearch = (e) => {
        e.preventDefault();
        setPage(1);
        fetchClients(1, searchData);
    };

    return (
        <div className="card">
            <div className="card-header">
                <h3>Список клиентов</h3>
            </div>
            <div className="card-body">
                <form className="row g-3 mb-3" onSubmit={handleSearch}>
                    <div className="col-md-2">
                        <input
                            type="text"
                            name="firstName"
                            className="form-control"
                            placeholder="Имя"
                            value={searchData.firstName}
                            onChange={handleInputChange}
                        />
                    </div>
                    <div className="col-md-2">
                        <input
                            type="text"
                            name="lastName"
                            className="form-control"
                            placeholder="Фамилия"
                            value={searchData.lastName}
                            onChange={handleInputChange}
                        />
                    </div>
                    <div className="col-md-2">
                        <input
                            type="text"
                            name="middleName"
                            className="form-control"
                            placeholder="Отчество"
                            value={searchData.middleName}
                            onChange={handleInputChange}
                        />
                    </div>
                    <div className="col-md-2">
                        <input
                            type="text"
                            name="phone"
                            className="form-control"
                            placeholder="Телефон"
                            value={searchData.phone}
                            onChange={handleInputChange}
                        />
                    </div>
                    <div className="col-md-2">
                        <input
                            type="text"
                            name="passport"
                            className="form-control"
                            placeholder="Паспорт"
                            value={searchData.passport}
                            onChange={handleInputChange}
                        />
                    </div>
                    <div className="col-md-2">
                        <button type="submit" className="btn btn-primary w-100">
                            Поиск
                        </button>
                    </div>
                </form>

                <table className="table table-bordered">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Имя</th>
                        <th>Фамилия</th>
                        <th>Отчество</th>
                        <th>Телефон</th>
                        <th>Паспорт</th>
                    </tr>
                    </thead>
                    <tbody>
                    {clients.map((client) => (
                        <tr key={client.id}>
                            <td>{client.id}</td>
                            <td>{client.firstName}</td>
                            <td>{client.lastName}</td>
                            <td>{client.middleName || '-'}</td>
                            <td>{client.phone}</td>
                            <td>{client.passport}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>

                <div className="d-flex justify-content-between align-items-center">
                    <button className="btn btn-secondary" onClick={handlePrev} disabled={page === 1}>
                        Назад
                    </button>
                    <span>Страница {page}</span>
                    <button
                        className="btn btn-secondary"
                        onClick={handleNext}
                        disabled={page * pageSize >= total}
                    >
                        Вперёд
                    </button>
                </div>
            </div>
        </div>
    );
}