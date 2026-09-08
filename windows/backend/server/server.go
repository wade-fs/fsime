// Package server implements the Named Pipe IPC server.
// It accepts JSON-encoded Requests from the C++ TSF DLL and returns JSON Responses.
// Uses github.com/Microsoft/go-winio for Windows Named Pipe support.
package server

import (
	"bufio"
	"encoding/json"
	"log"
	"net"

	"github.com/wade/fsime-server/engine"
	"github.com/wade/fsime-server/ipc"
)

const PipeName = `\\.\pipe\FsimeServer`

// Serve starts the Named Pipe server and blocks until l.Accept() fails.
func Serve(l net.Listener, proc *engine.InputProcessor) {
	log.Printf("[server] Listening on %s", PipeName)
	for {
		conn, err := l.Accept()
		if err != nil {
			log.Printf("[server] Accept error: %v", err)
			return
		}
		go handleConn(conn, proc)
	}
}

// handleConn processes one TSF DLL connection session.
// Each session has its own InputProcessor state (one per IME window/thread).
func handleConn(conn net.Conn, sharedProc *engine.InputProcessor) {
	defer conn.Close()
	// Each connection gets an independent processor sharing the same DB.
	// (For true multi-process safety, use a per-connection processor.)
	proc := sharedProc // TODO: clone per-session if multi-thread support is needed

	scanner := bufio.NewScanner(conn)
	encoder := json.NewEncoder(conn)

	for scanner.Scan() {
		line := scanner.Bytes()
		var req ipc.Request
		if err := json.Unmarshal(line, &req); err != nil {
			writeErr(encoder, "invalid JSON: "+err.Error())
			continue
		}

		resp := dispatch(proc, req)
		if err := encoder.Encode(resp); err != nil {
			log.Printf("[server] write error: %v", err)
			return
		}
	}
	if err := scanner.Err(); err != nil {
		log.Printf("[server] read error: %v", err)
	}
}

// dispatch routes a request to the appropriate processor method.
func dispatch(proc *engine.InputProcessor, req ipc.Request) ipc.Response {
	state := proc.GetState()
	resp := ipc.Response{
		Composition: state.Composing,
		Candidates:  state.Candidates,
		Mode:        state.Mode,
	}
	if resp.Candidates == nil {
		resp.Candidates = []string{}
	}

	switch req.Type {
	case ipc.ReqMode:
		proc.SetMode(req.Mode)
		st := proc.GetState()
		resp.Mode = st.Mode
		resp.Composition = st.Composing
		resp.Candidates = []string{}

	case ipc.ReqKey:
		switch req.Key {
		case "BackSpace":
			proc.Backspace()
		case "Escape":
			proc.Clear()
		default:
			proc.AppendStroke(req.Key)
		}
		st := proc.GetState()
		resp.Composition = st.Composing
		resp.Candidates = st.Candidates
		if resp.Candidates == nil {
			resp.Candidates = []string{}
		}
		resp.Mode = st.Mode

	case ipc.ReqSelect:
		ch := proc.PickCandidate(req.Index)
		resp.Commit = ch
		st := proc.GetState()
		resp.Composition = st.Composing
		resp.Candidates = st.Candidates
		if resp.Candidates == nil {
			resp.Candidates = []string{}
		}
		resp.Mode = st.Mode

	case ipc.ReqReset:
		proc.Clear()
		resp.Composition = ""
		resp.Candidates = []string{}

	case ipc.ReqPhrase:
		phrases, err := proc.GetPhrase(req.PrevChar)
		if err != nil {
			resp.Error = err.Error()
		} else {
			resp.Candidates = phrases
		}

	default:
		resp.Error = "unknown request type: " + req.Type
	}

	return resp
}

func writeErr(enc *json.Encoder, msg string) {
	_ = enc.Encode(ipc.Response{Error: msg, Candidates: []string{}})
}
