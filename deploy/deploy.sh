#!/usr/bin/env bash
# Run on the VM: bash deploy.sh ghcr.io/owner/image@sha256:<digest>
set -Eeuo pipefail
image=${1:?Supply an immutable GHCR image digest}
[[ $image =~ ^ghcr.io/[a-z0-9._/-]+@sha256:[a-f0-9]{64}$ ]] || { echo 'Invalid image digest' >&2; exit 2; }
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
deploy_home=${AGROTRENDS_HOME:-$HOME/agrotrends}
state="$deploy_home/backend-deploy"
export APP_ENV_FILE="$deploy_home/backend/.env"
[[ -f $APP_ENV_FILE ]] || { echo 'Backend .env is missing' >&2; exit 2; }
mkdir -p "$state"
exec 9>"$state/deploy.lock"
flock -w 300 9 || { echo 'Another backend deployment is running' >&2; exit 1; }
docker() { sudo -n docker "$@"; }
compose() {
    sudo -n env APP_IMAGE="$1" APP_ENV_FILE="$APP_ENV_FILE" docker compose --project-name backend \
        --env-file "$APP_ENV_FILE" --file "$2/compose.image.yml" "${@:3}"
}
ready() {
    local expected=$1 container
    for ((attempt=0; attempt<60; attempt++)); do
        container=$(compose "$expected" "$script_dir" ps -q app)
        if [[ -n $container ]] && \
            [[ $(docker inspect --format '{{.State.Running}}' "$container") == true ]] && \
            curl --fail --silent --max-time 5 http://127.0.0.1:8080/actuator/health | \
                python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "UP")' 2>/dev/null; then
            return 0
        fi
        sleep 3
    done
    return 1
}

# Capture the actual running image, including on the first migration from a local build.
container=$(docker ps -q --filter label=com.docker.compose.project=backend --filter label=com.docker.compose.service=app)
previous_image=''
previous_dir=$script_dir
if [[ -n $container ]]; then
    previous_image=$(docker inspect --format '{{.Image}}' "$container")
fi
if [[ -L $state/current && -f $state/current/compose.image.yml ]]; then
    previous_dir=$(readlink -f "$state/current")
fi
rollback() {
    local result=$?
    trap - ERR INT TERM
    if [[ -n $previous_image ]]; then
        echo 'Deployment failed; restoring the previous application image' >&2
        if compose "$previous_image" "$previous_dir" up -d --no-build --pull never app && ready "$previous_image"; then
            echo 'Previous image restored; database changes are NOT rolled back' >&2
        else
            echo 'ROLLBACK FAILED: inspect the VM immediately' >&2
        fi
    else
        echo 'Deployment failed; no previous application image exists' >&2
    fi
    exit "$((result == 0 ? 1 : result))"
}

# Download/validate before touching the running app. Root's Docker login is used for private GHCR packages.
docker pull "$image"
compose "$image" "$script_dir" config --quiet
trap rollback ERR INT TERM
compose "$image" "$script_dir" up -d --no-build --pull never app
ready "$image" || { echo 'Backend health check failed' >&2; false; }
printf '%s\n' "$image" > "$script_dir/image"
if [[ -L $state/current ]]; then
    ln -sfn "$(readlink -f "$state/current")" "$state/previous"
fi
ln -sfn "$script_dir" "$state/current.next"
mv -Tf "$state/current.next" "$state/current"
trap - ERR INT TERM
echo 'Backend deployment healthy'
