#!/usr/bin/env bash
# Builds production server Docker image

__execute_local() {
    # short circuit if any command fails (see https://stackoverflow.com/a/4346420)
    set -e -o pipefail
    # see https://intoli.com/blog/exit-on-errors-in-bash-scripts/
    trap 'EXIT_CODE=$? && [[ ${EXIT_CODE} -ne 0 ]] && (echo "Failed to build image" && exit ${EXIT_CODE})' ERR

    # script directory (see https://stackoverflow.com/a/246128)
    local DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

    # read project pom.xml for version
    echo "Reading pom.xml"
    local version=$(cd .. && mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
    echo "Project version: ${version}"

    # build project
    echo "Building docker image"
    docker build \
        `# tag image (see https://stackoverflow.com/a/3545363)` \
        --tag "concision/unpacker:${version}" \
        `# specify production dockerfile` \
        --file "${DIR}/Dockerfile" \
        `# set build target if specified` \
        $([[ $# -ne 0  ]] && echo --target "${1}") \
        `# normalize path to workspace root` \
        "${DIR}/.."
}


# check if NOT inside a GitLab CI
if [[ -z "${GITLAB_CI}" ]]; then
    __execute_local "$@"
else
    echo Unsupported CI
    exit 1
fi

# clean up environment
unset __execute_local __execute_kaniko
