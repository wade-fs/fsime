// Package ipc defines the Named Pipe protocol between the C++ TSF DLL and Go server.
// All messages are newline-delimited JSON.
package ipc

// --- Client → Server ---

// RequestType constants for incoming messages.
const (
	ReqKey     = "key"     // user pressed a key
	ReqSelect  = "select"  // user selected candidate by index
	ReqCommit  = "commit"  // composition committed (for bigram learning)
	ReqReset   = "reset"   // clear current composition
	ReqPhrase  = "phrase"  // request associated phrase suggestions
	ReqMode    = "mode"    // switch input mode (boshiamy/ji/cj/stroke/digit)
)

// Request is a single message from the TSF DLL to the Go server.
type Request struct {
	Type     string `json:"type"`            // one of ReqXxx constants
	Key      string `json:"key,omitempty"`   // printable char or special key name
	Index    int    `json:"index,omitempty"` // candidate index (for ReqSelect)
	PrevChar string `json:"prev,omitempty"`  // last committed char (for ReqCommit learning)
	Mode     string `json:"mode,omitempty"`  // input mode (for ReqMode)
}

// --- Server → Client ---

// Response is sent back to the TSF DLL after each request.
type Response struct {
	// Composition is the current pending input string shown in the composition window.
	Composition string `json:"composition"`

	// Candidates is the ordered list of candidate characters/words.
	Candidates []string `json:"candidates"`

	// Commit is a non-empty string when a character should be committed to the app.
	Commit string `json:"commit,omitempty"`

	// Mode reflects the current active input mode.
	Mode string `json:"mode"`

	// Error is non-empty when the server encountered an error.
	Error string `json:"error,omitempty"`
}
