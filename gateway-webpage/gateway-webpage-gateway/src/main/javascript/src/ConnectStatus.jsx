import React, { useState } from 'react';
import Select from 'react-select';

const ConnectOverview = () => {
    const options = [
        { value: "TrackAndTrace", label: "Track And Trace" },
        { value: "Quality", label: "Quality" },
        { value: "DocumentManager", label: "Doc Management" }
    ];

    const [features, setFeatures] = useState([]);

    const handleChange = (selectedFeatures) => {
        setFeatures(selectedFeatures || []);
    };

    const install = () => {
        console.log("Selected features: ");
        let params = features.map((a) => a.value);
        console.log(params);

        // Convert params array to a query string
        let queryString = `?params=${params.join(',')}`;

        fetch(`/data/hce/install/${queryString}`, {
            method: 'GET',
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
    };

    const showConfirmation = () => {
        console.log("Install clicked");

        // Generate confirmation message
        let confirmationMessage = "Are you sure you want to install ";
        if (features.length === 1) {
            confirmationMessage += `${features[0].label}?`;
        } else if (features.length > 1) {
            const featureLabels = features.map(feature => feature.label);
            const lastFeature = featureLabels.pop(); // Remove last element from array

            confirmationMessage += featureLabels.join(', ') + ` and ${lastFeature}?`;
        } else {
            confirmationMessage += "these features?";
        }

        // Display confirmation dialog
        if (window.confirm(confirmationMessage)) {
            console.log("Installing");
            install();
        } else {
            console.log("Install cancelled");
        }
    };

    return (
        <div>
            <div>
                <div>
                    <Select
                        options={options}
                        onChange={handleChange}
                        value={features}
                        isMulti
                    />
                    <button
                        style={{
                            padding: '10px 20px',
                            backgroundColor: '#2199e8',
                            color: 'white',
                            border: 'none',
                            borderRadius: '5px',
                            cursor: 'pointer',
                            fontSize: '16px',
                            fontWeight: 'bold'
                        }}
                        onClick={showConfirmation}
                    >
                        Install
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ConnectOverview;