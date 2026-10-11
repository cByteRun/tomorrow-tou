# Source only after changing to the project root.
API_PORT="$(python3 scripts/api-port.py)" || exit 1
export API_PORT
TOU_API_URL="${TOU_API_URL:-http://localhost:$API_PORT}"
export TOU_API_URL
