import React, { useState, useEffect } from 'react';
import 'bootstrap/dist/css/bootstrap.min.css';
import { Container, Navbar, Nav, Card, Button, Form, Row, Col, Tab, Tabs, Alert, Badge, Modal } from 'react-bootstrap';
import axios from 'axios';

// Types
interface User {
  id: string;
  email: string;
  name: string;
}

interface Event {
  id: string;
  name: string;
  rows: number;
  cols: number;
  seats: Record<string, string>; // seatId -> status
}

interface CartItem {
  eventId: string;
  seatId: string;
  unitPriceCents: number;
}

const API_BASE = ''; // Relative path handled by proxy

const App: React.FC = () => {
  // Auth State
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  
  // UI State
  const [activeTab, setActiveTab] = useState<string>('login');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Forms
  const [loginEmail, setLoginEmail] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regName, setRegName] = useState('');
  
  const [evtName, setEvtName] = useState('');
  const [evtRows, setEvtRows] = useState(5);
  const [evtCols, setEvtCols] = useState(10);
  const [loadEvtId, setLoadEvtId] = useState('');

  // App Data
  const [currentEvent, setCurrentEvent] = useState<Event | null>(null);
  const [selectedSeats, setSelectedSeats] = useState<string[]>([]);
  const [currentOrderId, setCurrentOrderId] = useState<number | null>(null);
  const [orderTotal, setOrderTotal] = useState<number>(0);

  // Auth Handlers
  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null); setSuccess(null);
    try {
      await axios.post(`${API_BASE}/api/users`, { email: regEmail, name: regName });
      setSuccess('Registered! Please login.');
      setActiveTab('login');
      setLoginEmail(regEmail);
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Registration failed');
    }
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const res = await axios.post(`${API_BASE}/api/login`, { email: loginEmail });
      setToken(res.data.token);
      // Fetch full user details? Assuming ID is returned or token is enough. 
      // Mock login returns { token, userId }
      const userId = res.data.userId;
      setUser({ id: userId, email: loginEmail, name: 'User' }); // Mock name
      setActiveTab('dashboard');
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Login failed');
    }
  };

  const handleLogout = () => {
    setUser(null);
    setToken(null);
    setCurrentEvent(null);
    setCurrentOrderId(null);
    setActiveTab('login');
  };

  // Event Handlers
  const createEvent = async () => {
    if (!user) return;
    setError(null); setSuccess(null);
    try {
      const res = await axios.post(`${API_BASE}/api/events`, {
        name: evtName,
        rows: evtRows,
        cols: evtCols,
        userId: user.id
      });
      setSuccess(`Event Created! ID: ${res.data.id}`);
      setLoadEvtId(res.data.id);
      loadEvent(res.data.id);
    } catch (err: any) {
      setError('Failed to create event');
    }
  };

  const loadEvent = async (id: string) => {
    setError(null);
    try {
      const res = await axios.get(`${API_BASE}/api/events/${id}`);
      setCurrentEvent(res.data);
      setSelectedSeats([]);
    } catch (err: any) {
      setError('Event not found');
    }
  };

  // Seat Logic
  const toggleSeat = (seatId: string) => {
    if (!currentEvent) return;
    const status = currentEvent.seats[seatId];
    if (status !== 'available') return;

    if (selectedSeats.includes(seatId)) {
      setSelectedSeats(prev => prev.filter(s => s !== seatId));
    } else {
      if (selectedSeats.length >= 8) {
        setError('Max 8 seats');
        return;
      }
      setSelectedSeats(prev => [...prev, seatId]);
    }
  };

  const reserveSeats = async () => {
    if (!user || !currentEvent || selectedSeats.length === 0) return;
    setError(null);
    try {
      // 1. Reserve at Event Service
      await axios.post(`${API_BASE}/api/events/${currentEvent.id}/reserve`, {
        userId: user.id,
        seats: selectedSeats
      });

      // 2. Create Order
      const items: CartItem[] = selectedSeats.map(seatId => ({
        eventId: currentEvent.id,
        seatId: seatId,
        unitPriceCents: 1000
      }));

      const orderRes = await axios.post(`${API_BASE}/api/orders`, {
        userId: user.id,
        status: 'IN_CART',
        items: items
      });

      setCurrentOrderId(orderRes.data.id);
      setOrderTotal(items.length * 10); // $10 each
      
      // Refresh map to show reserved
      loadEvent(currentEvent.id);
      setSuccess('Seats Reserved! Please finalize payment.');
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Reservation failed');
      loadEvent(currentEvent.id); // Reload to sync
    }
  };

  const finalizeOrder = async () => {
    if (!currentOrderId) return;
    setError(null); setSuccess(null);
    try {
      await axios.post(`${API_BASE}/api/orders/finalize/${currentOrderId}`);
      setSuccess('Payment Submitted! Processing...');
      setCurrentOrderId(null);
      setSelectedSeats([]);
      
      // Poll for confirmation
      let checks = 0;
      const interval = setInterval(() => {
        if(currentEvent) loadEvent(currentEvent.id);
        checks++;
        if (checks > 5) clearInterval(interval);
      }, 2000);
    } catch (err: any) {
      setError('Finalize failed');
    }
  };

  // Render Helpers
  const renderSeatMap = () => {
    if (!currentEvent) return null;
    const rows = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(0, currentEvent.rows);
    
    return (
      <div className="d-inline-block p-3 border rounded bg-white text-center">
        <div className="mb-3 bg-dark text-white py-2 rounded">STAGE</div>
        {rows.split('').map(rowLetter => (
          <div key={rowLetter} className="d-flex justify-content-center mb-1">
            {Array.from({ length: currentEvent.cols }, (_, i) => i + 1).map(col => {
              const seatId = `${rowLetter}${col}`;
              const status = currentEvent.seats[seatId];
              const isSelected = selectedSeats.includes(seatId);
              
              let bgClass = 'bg-success'; // available
              if (status === 'reserved') bgClass = 'bg-warning text-dark';
              if (status === 'confirmed') bgClass = 'bg-danger';
              if (isSelected) bgClass = 'bg-primary';

              return (
                <div 
                  key={seatId}
                  className={`m-1 d-flex align-items-center justify-content-center text-white fw-bold rounded ${bgClass}`}
                  style={{ width: '35px', height: '35px', cursor: status === 'available' ? 'pointer' : 'not-allowed' }}
                  onClick={() => toggleSeat(seatId)}
                  title={`Seat ${seatId}`}
                >
                  {seatId}
                </div>
              );
            })}
          </div>
        ))}
        <div className="mt-3">
          <Badge bg="success" className="me-2">Available</Badge>
          <Badge bg="primary" className="me-2">Selected</Badge>
          <Badge bg="warning" text="dark" className="me-2">Reserved</Badge>
          <Badge bg="danger">Sold</Badge>
        </div>
      </div>
    );
  };

  return (
    <div className="bg-light min-vh-100">
      <Navbar bg="dark" variant="dark" expand="lg" className="mb-4">
        <Container>
          <Navbar.Brand href="#">TicketChief</Navbar.Brand>
          <Navbar.Toggle aria-controls="basic-navbar-nav" />
          <Navbar.Collapse className="justify-content-end">
            {user ? (
              <Nav>
                <Navbar.Text className="me-3">Signed in as: {user.email}</Navbar.Text>
                <Button variant="outline-light" size="sm" onClick={handleLogout}>Logout</Button>
              </Nav>
            ) : (
              <Navbar.Text>Please Login</Navbar.Text>
            )}
          </Navbar.Collapse>
        </Container>
      </Navbar>

      <Container>
        {error && <Alert variant="danger" onClose={() => setError(null)} dismissible>{error}</Alert>}
        {success && <Alert variant="success" onClose={() => setSuccess(null)} dismissible>{success}</Alert>}

        {!user ? (
          <Card className="mx-auto shadow-sm" style={{ maxWidth: '400px' }}>
            <Card.Body>
              <Tabs activeKey={activeTab} onSelect={(k) => setActiveTab(k || 'login')} className="mb-3">
                <Tab eventKey="login" title="Login">
                  <Form onSubmit={handleLogin}>
                    <Form.Group className="mb-3">
                      <Form.Label>Email</Form.Label>
                      <Form.Control type="email" value={loginEmail} onChange={e => setLoginEmail(e.target.value)} required />
                    </Form.Group>
                    <Button variant="primary" type="submit" className="w-100">Login</Button>
                  </Form>
                </Tab>
                <Tab eventKey="register" title="Register">
                  <Form onSubmit={handleRegister}>
                    <Form.Group className="mb-3">
                      <Form.Label>Email</Form.Label>
                      <Form.Control type="email" value={regEmail} onChange={e => setRegEmail(e.target.value)} required />
                    </Form.Group>
                    <Form.Group className="mb-3">
                      <Form.Label>Name</Form.Label>
                      <Form.Control type="text" value={regName} onChange={e => setRegName(e.target.value)} required />
                    </Form.Group>
                    <Button variant="success" type="submit" className="w-100">Register</Button>
                  </Form>
                </Tab>
              </Tabs>
            </Card.Body>
          </Card>
        ) : (
          <Row>
            <Col md={4}>
              <Card className="mb-4 shadow-sm">
                <Card.Header>Create Event</Card.Header>
                <Card.Body>
                  <Form.Group className="mb-2">
                    <Form.Control type="text" placeholder="Event Name" value={evtName} onChange={e => setEvtName(e.target.value)} />
                  </Form.Group>
                  <Row className="g-2 mb-2">
                    <Col><Form.Control type="number" placeholder="Rows" value={evtRows} onChange={e => setEvtRows(parseInt(e.target.value))} /></Col>
                    <Col><Form.Control type="number" placeholder="Cols" value={evtCols} onChange={e => setEvtCols(parseInt(e.target.value))} /></Col>
                  </Row>
                  <Button variant="success" className="w-100" onClick={createEvent}>Create</Button>
                </Card.Body>
              </Card>

              <Card className="mb-4 shadow-sm">
                <Card.Header>Load Event</Card.Header>
                <Card.Body>
                  <div className="d-flex">
                    <Form.Control type="text" placeholder="Event ID" value={loadEvtId} onChange={e => setLoadEvtId(e.target.value)} className="me-2" />
                    <Button onClick={() => loadEvent(loadEvtId)}>Load</Button>
                  </div>
                </Card.Body>
              </Card>

              {currentOrderId && (
                <Card className="mb-4 shadow-sm border-primary">
                  <Card.Header className="bg-primary text-white">Checkout</Card.Header>
                  <Card.Body>
                    <p className="mb-2"><strong>Order ID:</strong> {currentOrderId}</p>
                    <p className="mb-3"><strong>Total:</strong> ${orderTotal.toFixed(2)}</p>
                    <Button variant="warning" className="w-100 fw-bold" onClick={finalizeOrder}>Pay & Finalize</Button>
                  </Card.Body>
                </Card>
              )}
            </Col>

            <Col md={8}>
              {currentEvent ? (
                <Card className="shadow-sm">
                  <Card.Header className="d-flex justify-content-between align-items-center">
                    <h5 className="mb-0">{currentEvent.name}</h5>
                    <Badge bg="secondary">{currentEvent.id}</Badge>
                  </Card.Header>
                  <Card.Body className="text-center">
                    {renderSeatMap()}
                    <div className="mt-4">
                      <Button 
                        size="lg" 
                        disabled={selectedSeats.length === 0 || !!currentOrderId} 
                        onClick={reserveSeats}
                      >
                        {currentOrderId ? 'Complete Checkout' : `Reserve ${selectedSeats.length} Seats`}
                      </Button>
                    </div>
                  </Card.Body>
                </Card>
              ) : (
                <div className="text-center text-muted mt-5 pt-5">
                  <h3>Select or Create an Event</h3>
                  <p>Use the sidebar to get started.</p>
                </div>
              )}
            </Col>
          </Row>
        )}
      </Container>
    </div>
  );
}

export default App;
