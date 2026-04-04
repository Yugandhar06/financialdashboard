import React, { useEffect, useState, useContext } from 'react';
import { AuthContext } from '../../App';
import { transactionService } from '../../api/transactionService';
import { Search, Loader2, Plus, Edit2, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import TransactionModal from './TransactionModal';
import './Transactions.css';

export default function Transactions() {
    const { user } = useContext(AuthContext);
    const [data, setData] = useState({ content: [], totalPages: 0 });
    const [loading, setLoading] = useState(true);

    // Modal states
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedTx, setSelectedTx] = useState(null);

    // Filters
    const [page, setPage] = useState(0);
    const [type, setType] = useState('');
    const [category, setCategory] = useState('');

    const isAdmin = user?.role === 'ADMIN';

    const loadTransactions = async () => {
        setLoading(true);
        try {
            const resData = await transactionService.getTransactions({
                page,
                size: 10,
                type: type || undefined,
                category: category || undefined
            });
            setData(resData.data || { content: [], totalPages: 0 });
        } catch (e) {
            toast.error("Failed to load transactions.");
            console.error("Failed to load transactions", e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadTransactions();
    }, [page, type, category]);

    const handleDelete = async (id) => {
        if (!window.confirm("Are you sure you want to delete this transaction?")) return;

        try {
            await transactionService.deleteTransaction(id);
            toast.success("Transaction deleted successfully.");
            // Refresh list, handling case where last item on page is deleted
            if (data.content.length === 1 && page > 0) {
                setPage(p => p - 1);
            } else {
                loadTransactions();
            }
        } catch (e) {
            toast.error(e.message || "Failed to delete transaction.");
        }
    };

    const openNewModal = () => {
        setSelectedTx(null);
        setIsModalOpen(true);
    };

    const openEditModal = (tx) => {
        setSelectedTx(tx);
        setIsModalOpen(true);
    };

    return (
        <div className="tx-container">
            <header className="tx-header">
                <div>
                    <h1 className="tx-title">Transactions</h1>
                    <p className="tx-subtitle">View and trace all financial records.</p>
                </div>
                {isAdmin && (
                    <button className="btn-primary" onClick={openNewModal}>
                        <Plus size={20} strokeWidth={2.5} /> New Transaction
                    </button>
                )}
            </header>

            <div className="glass-panel tx-panel">
                <div className="tx-controls">
                    <div className="tx-search-box">
                        <Search size={18} className="tx-search-icon" />
                        <input
                            type="text"
                            placeholder="Search by category..."
                            className="input-base"
                            style={{ paddingLeft: '40px' }}
                            value={category}
                            onChange={(e) => { setCategory(e.target.value); setPage(0); }}
                        />
                    </div>

                    <select
                        className="input-base tx-filter"
                        value={type}
                        onChange={(e) => { setType(e.target.value); setPage(0); }}
                    >
                        <option value="">All Types</option>
                        <option value="INCOME">Income Only</option>
                        <option value="EXPENSE">Expense Only</option>
                    </select>
                </div>

                {loading ? (
                    <div className="tx-loading">
                        <Loader2 className="spinner" size={32} />
                        <p>Loading records...</p>
                    </div>
                ) : (
                    <div className="tx-table-wrapper">
                        <table>
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Category</th>
                                    <th>Type</th>
                                    <th style={{ textAlign: 'right' }}>Amount</th>
                                    {isAdmin && <th style={{ textAlign: 'right' }}>Actions</th>}
                                </tr>
                            </thead>
                            <tbody>
                                {(!data.content || data.content.length === 0) ? (
                                    <tr>
                                        <td colSpan={isAdmin ? 5 : 4} className="tx-empty">No transactions found matching your filters.</td>
                                    </tr>
                                ) : (
                                    data.content.map(tx => (
                                        <tr key={tx.id}>
                                            <td>{new Date(tx.date).toLocaleDateString()}</td>
                                            <td style={{ fontWeight: 500 }}>{tx.category}</td>
                                            <td>
                                                <span className={`tx-badge ${tx.type === 'INCOME' ? 'badge-income' : 'badge-expense'}`}>
                                                    {tx.type}
                                                </span>
                                            </td>
                                            <td style={{
                                                textAlign: 'right',
                                                fontWeight: 600,
                                                color: tx.type === 'INCOME' ? 'var(--success)' : 'var(--text-primary)'
                                            }}>
                                                ${tx.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                                            </td>
                                            {isAdmin && (
                                                <td>
                                                    <div className="tx-row-actions">
                                                        <button className="btn-icon" onClick={() => openEditModal(tx)} title="Edit">
                                                            <Edit2 size={16} />
                                                        </button>
                                                        <button className="btn-icon danger" onClick={() => handleDelete(tx.id)} title="Delete">
                                                            <Trash2 size={16} />
                                                        </button>
                                                    </div>
                                                </td>
                                            )}
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* Pagination Controls */}
                {data.totalPages > 1 && (
                    <div className="tx-pagination">
                        <button
                            disabled={page === 0}
                            onClick={() => setPage(p => p - 1)}
                            className="btn-page"
                        >
                            Previous
                        </button>
                        <span className="tx-page-info">Page {page + 1} of {data.totalPages}</span>
                        <button
                            disabled={page >= data.totalPages - 1}
                            onClick={() => setPage(p => p + 1)}
                            className="btn-page"
                        >
                            Next
                        </button>
                    </div>
                )}
            </div>

            <TransactionModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                transaction={selectedTx}
                onSuccess={loadTransactions}
            />
        </div>
    );
}
