const express = require("express");
const multer = require("multer");
const AdmZip = require("adm-zip");
const fs = require("node:fs");
const path = require("node:path");
const crypto = require("node:crypto");
const { spawn } = require("node:child_process");

const app = express();
const PORT = process.env.PORT || 8787;
const WORK_ROOT = path.join(__dirname, "workspaces");
fs.mkdirSync(WORK_ROOT, { recursive: true });

const upload = multer({
  dest: path.join(WORK_ROOT, "_uploads"),
  limits: { fileSize: 100 * 1024 * 1024 }
});

const builds = new Map();

function detectAndroidSdk() {
  const sdk = process.env.ANDROID_SDK_ROOT || process.env.ANDROID_HOME || "";
  return Boolean(sdk && fs.existsSync(sdk));
}

app.get("/health", (_req, res) => {
  res.json({
    ok: true,
    message: "Code X Gradle Build Server operacional",
    androidSdk: detectAndroidSdk(),
    gradleVersion: process.env.GRADLE_BIN || "gradle"
  });
});

app.post("/api/builds", upload.single("projectZip"), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: "Arquivo projectZip ausente." });
  }

  const buildId = crypto.randomUUID();
  const buildType = (req.body.buildType || "DEBUG").toUpperCase();
  const projectName = req.body.projectName || "App";
  const workDir = path.join(WORK_ROOT, buildId);
  fs.mkdirSync(workDir, { recursive: true });

  const state = {
    buildId,
    projectName,
    buildType,
    status: "running",
    logs: `[Code X Build Server] Extraindo projeto '${projectName}' (${buildType})...\n`,
    apkPath: null,
    error: null
  };
  builds.set(buildId, state);

  try {
    const zip = new AdmZip(req.file.path);
    zip.extractAllTo(workDir, true);
    fs.unlinkSync(req.file.path);
  } catch (err) {
    state.status = "failed";
    state.error = `Erro ao extrair ZIP: ${err.message}`;
    state.logs += `${state.error}\n`;
    return res.status(500).json({ buildId, error: state.error });
  }

  const gradleTask = buildType === "RELEASE" ? "assembleRelease" : "assembleDebug";
  const gradleCmd = fs.existsSync(path.join(workDir, "gradlew"))
    ? "./gradlew"
    : process.env.GRADLE_BIN || "gradle";

  if (gradleCmd === "./gradlew") {
    fs.chmodSync(path.join(workDir, "gradlew"), 0o755);
  }

  state.logs += `[Code X Build Server] Executando: ${gradleCmd} ${gradleTask} --no-daemon\n`;

  const proc = spawn(gradleCmd, [gradleTask, "--no-daemon", "--stacktrace"], {
    cwd: workDir,
    env: process.env
  });

  proc.stdout.on("data", (chunk) => {
    state.logs += chunk.toString();
  });
  proc.stderr.on("data", (chunk) => {
    state.logs += chunk.toString();
  });

  proc.on("close", (code) => {
    if (code !== 0) {
      state.status = "failed";
      state.error = `Gradle finalizou com código ${code}.`;
      state.logs += `\n[FALHA] ${state.error}\n`;
      return;
    }

    const apkFolder = path.join(
      workDir,
      "app",
      "build",
      "outputs",
      "apk",
      buildType === "RELEASE" ? "release" : "debug"
    );
    const apkFile = findFirstApk(apkFolder);
    if (!apkFile) {
      state.status = "failed";
      state.error = "Build concluído, mas nenhum arquivo .apk foi encontrado em outputs/apk.";
      state.logs += `\n[FALHA] ${state.error}\n`;
      return;
    }

    state.apkPath = apkFile;
    state.status = "success";
    state.logs += `\n[SUCESSO] APK gerado: ${path.basename(apkFile)}\n`;
  });

  proc.on("error", (err) => {
    state.status = "failed";
    state.error = `Erro ao iniciar Gradle: ${err.message}`;
    state.logs += `\n[FALHA] ${state.error}\n`;
  });

  return res.status(202).json({ buildId, status: state.status });
});

app.get("/api/builds/:buildId", (req, res) => {
  const state = builds.get(req.params.buildId);
  if (!state) return res.status(404).json({ error: "Build não encontrado." });

  res.json({
    buildId: state.buildId,
    status: state.status,
    logs: state.logs,
    apkUrl: state.apkPath ? `/api/builds/${state.buildId}/apk` : null,
    error: state.error
  });
});

app.get("/api/builds/:buildId/apk", (req, res) => {
  const state = builds.get(req.params.buildId);
  if (!state || !state.apkPath || !fs.existsSync(state.apkPath)) {
    return res.status(404).json({ error: "APK não disponível para este build." });
  }
  return res.download(state.apkPath, path.basename(state.apkPath));
});

function findFirstApk(dir) {
  if (!fs.existsSync(dir)) return null;
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isFile() && entry.name.endsWith(".apk")) return full;
    if (entry.isDirectory()) {
      const nested = findFirstApk(full);
      if (nested) return nested;
    }
  }
  return null;
}

app.listen(PORT, "0.0.0.0", () => {
  console.log(`Code X Gradle Build Server ouvindo em http://0.0.0.0:${PORT}`);
});
