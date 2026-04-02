import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import toast from 'react-hot-toast';
import { transactionService } from '../../api/transactionService';

export default function TransactionModal({ isOpen, onClose, transaction, onSuccess }) {
  const [formData, setFormData] = useState({
    amount: '',
    date: new Date().toISOString().split('T')[0],
    category: '',
    type: 'EXPENSE',
    description: '' // optional
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (transaction) {
      setFormData({
        amount: transaction.amount,
        date: new Date(transaction.date).toISOString().split('T')[0],
        category: transaction.category,
        type: transaction.type,
        description: transaction.description || ''
      });
    } else {
      setFormData({
        amount: '',
        date: new Date().toISOString().split('T')[0],
        category: '',
        type: 'EXPENSE',
        description: ''
      });
    }
  }, [transaction, isOpen]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    
    // Convert amount to number
    const payload = {
      ...formData,
      amount: parseFloat(formData.amount)
    };

    try {
      if (transaction) {
        await transactionService.updateTransaction(transaction.id, payload);
        toast.success("Transaction updated successfully!");
      } else {
        await transactionService.createTransaction(payload);
        toast.success("Transaction created successfully!");
      }
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err.message || "Failed to save transaction.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="tx-modal-overlay">
      <div className="glass-panel tx-modal">
        <div className="tx-modal-header">
          <h2>{transaction ? 'Edit Transaction' : 'New Transaction'}</h2>
          <button className="tx-modal-close" onClick={onClose}>
            <X size={20} />
          </button>
        </div>
        
        <form onSubmit={handleSubmit} className="tx-modal-form">
          <div className="form-group">
            <label>Amount *</label>
            <input 
              type="number" 
              name="amount"
              step="0.01"
              required 
              className="input-base" 
              placeholder="0.00"
              value={formData.amount}
              onChange={handleChange}
            />
          </div>

          <div className="form-group">
            <label>Date *</label>
            <input 
              type="date" 
              name="date"
              required 
              className="input-base" 
              value={formData.date}
              onChange={handleChange}
            />
          </div>

          <div className="form-group">
            <label>Category *</label>
            <input 
              type="text" 
              name="category"
              required 
              className="input-base" 
              placeholder="e.g. Groceries, Salary, Rent"
              value={formData.category}
              onChange={handleChange}
            />
          </div>

          <div className="form-group">
            <label>Type *</label>
            <select 
              name="type" 
              className="input-base" 
              value={formData.type}
              onChange={handleChange}
            >
              <option value="EXPENSE">Expense</option>
              <option value="INCOME">Income</option>
            </select>
          </div>

          <div className="form-actions">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Saving...' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
