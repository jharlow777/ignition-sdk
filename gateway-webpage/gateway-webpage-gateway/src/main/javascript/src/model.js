import {combineReducers} from 'redux';

function getConnections(state = null) {
    return state;
}

const reducer = combineReducers({getConnections});
export default reducer;

export function modelInstall() {
    return function () {
        fetch(`/data/hce/install`, {
            method: 'get',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        })
        .then(response => response.json())
        .then(json => {
            console.log("JSON Response: ");
            console.log(json);
        });
    }
}
