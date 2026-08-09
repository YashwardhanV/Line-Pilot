import { AppShell } from "./components/AppShell";
import { CustomerPage } from "./pages/CustomerPage";
import { StaffPage } from "./pages/StaffPage";

export default function App() {
  const page = window.location.pathname.startsWith("/staff") ? <StaffPage /> : <CustomerPage />;
  return <AppShell>{page}</AppShell>;
}
