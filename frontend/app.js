// All calls go to /api/... — nginx (nginx.conf) proxies them to the backend container.
const API = "/api";

const statusEl = document.getElementById("status");
const listEl = document.getElementById("task-list");
const formEl = document.getElementById("task-form");
const inputEl = document.getElementById("task-input");

async function checkHealth() {
  try {
    const res = await fetch(`${API}/health`);
    const data = await res.json();
    statusEl.textContent = `backend: ${data.status} | db: ${data.db}`;
    statusEl.className = data.db === "connected" ? "status ok" : "status err";
  } catch {
    statusEl.textContent = "backend: unreachable";
    statusEl.className = "status err";
  }
}

async function loadTasks() {
  try {
    const res = await fetch(`${API}/tasks`);
    const tasks = await res.json();
    listEl.innerHTML = "";
    tasks.forEach(renderTask);
  } catch {
    listEl.innerHTML = "<li>could not load tasks</li>";
  }
}

function renderTask(task) {
  const li = document.createElement("li");
  if (task.done) li.classList.add("done");

  const span = document.createElement("span");
  span.textContent = task.title;
  span.onclick = () => toggleTask(task.id, !task.done);

  const del = document.createElement("button");
  del.className = "del";
  del.textContent = "x";
  del.onclick = () => deleteTask(task.id);

  li.append(span, del);
  listEl.appendChild(li);
}

async function addTask(title) {
  await fetch(`${API}/tasks`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title }),
  });
  loadTasks();
}

async function toggleTask(id, done) {
  await fetch(`${API}/tasks/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ done }),
  });
  loadTasks();
}

async function deleteTask(id) {
  await fetch(`${API}/tasks/${id}`, { method: "DELETE" });
  loadTasks();
}

formEl.addEventListener("submit", (e) => {
  e.preventDefault();
  const title = inputEl.value.trim();
  if (title) addTask(title);
  inputEl.value = "";
});

checkHealth();
loadTasks();
