#!/usr/bin/env bash
set -euo pipefail

echo "=========================================="
echo "  Project Setup Script"
echo "=========================================="

# -------------------------------
# Ensure script runs from repo root
# -------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# -------------------------------
# Colors
# -------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# -------------------------------
# Tool checks
# -------------------------------
command -v git >/dev/null || {
  echo -e "${RED}Error: git is not installed${NC}"
  exit 1
}

command -v java >/dev/null || {
  echo -e "${RED}Error: Java is not installed${NC}"
  exit 1
}

# -------------------------------
# Java version check (17+)
# -------------------------------
echo -e "${YELLOW}Checking Java version...${NC}"
java_version=$(java -version 2>&1 | head -n 1 | sed -E 's/.*"([0-9]+).*/\1/')
if [ "$java_version" -lt 17 ]; then
  echo -e "${RED}Error: Java 17+ required. Found Java $java_version${NC}"
  exit 1
fi
echo -e "${GREEN}✓ Java $java_version detected${NC}"

# -------------------------------
# Git submodules (including nested)
# -------------------------------
echo -e "${YELLOW}Initializing git submodules...${NC}"

if [ -f .gitmodules ]; then
  git submodule sync --recursive
  git submodule update --init --recursive --force
else
  echo -e "${YELLOW}No .gitmodules found, skipping submodules${NC}"
fi

echo -e "${YELLOW}Submodule status:${NC}"
git submodule status --recursive || true

echo -e "${GREEN}✓ Submodules initialized${NC}"

# -------------------------------
# libxposed setup
# -------------------------------
LIBXPOSED_DIR="libxposed"

mkdir -p "$LIBXPOSED_DIR"

if [ ! -d "$LIBXPOSED_DIR/api/.git" ]; then
  echo -e "${YELLOW}Cloning libxposed/api...${NC}"
  git clone https://github.com/libxposed/api.git "$LIBXPOSED_DIR/api"
else
  echo -e "${GREEN}✓ libxposed/api already exists${NC}"
fi

if [ ! -d "$LIBXPOSED_DIR/service/.git" ]; then
  echo -e "${YELLOW}Cloning libxposed/service...${NC}"
  git clone https://github.com/libxposed/service.git "$LIBXPOSED_DIR/service"
else
  echo -e "${GREEN}✓ libxposed/service already exists${NC}"
fi

# -------------------------------
# Gradle wrapper permissions
# -------------------------------
chmod +x ./gradlew || true
chmod +x "$LIBXPOSED_DIR/api/gradlew"
chmod +x "$LIBXPOSED_DIR/service/gradlew"

# -------------------------------
# Publish libxposed to Maven local
# -------------------------------
echo -e "${YELLOW}Publishing libxposed/api to local Maven...${NC}"
(
  cd "$LIBXPOSED_DIR/api"
  ./gradlew :api:publishApiPublicationToMavenLocal --quiet
)
echo -e "${GREEN}✓ libxposed/api published${NC}"

echo -e "${YELLOW}Publishing libxposed/service to local Maven...${NC}"
(
  cd "$LIBXPOSED_DIR/service"
  ./gradlew :interface:publishInterfacePublicationToMavenLocal --quiet
)
echo -e "${GREEN}✓ libxposed/service published${NC}"

# -------------------------------
# Verification
# -------------------------------
echo -e "${YELLOW}Verifying local Maven artifacts...${NC}"

if [ -d "$HOME/.m2/repository/io/github/libxposed/api" ] \
   && [ -d "$HOME/.m2/repository/io/github/libxposed/interface" ]; then
  echo -e "${GREEN}✓ libxposed dependencies verified in local Maven${NC}"
else
  echo -e "${RED}Warning: libxposed artifacts not found in local Maven${NC}"
fi

# -------------------------------
# Done
# -------------------------------
echo ""
echo -e "${GREEN}=========================================="
echo "  Setup Complete!"
echo "==========================================${NC}"
echo ""
echo "You can now build the project with:"
echo "  ./gradlew buildDebug"
echo ""
