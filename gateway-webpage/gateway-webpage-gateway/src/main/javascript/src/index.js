import React from 'react';
import ConnectOverview from './ConnectStatus.jsx';
import { createStore, applyMiddleware } from 'redux';
import { Provider } from 'react-redux';
import thunkMiddleware from 'redux-thunk';

import reducer from './model.js';

const createStoreWithMiddleware = applyMiddleware(thunkMiddleware)(createStore);
const store = createStoreWithMiddleware(reducer);

const MountableApp = class StatusPageApp extends React.Component {
    render() {
        return <Provider store={store}><ConnectOverview dispatch={store.dispatch}/></Provider>
    }
}

export default MountableApp;
